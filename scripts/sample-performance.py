"""One development 20 TPS sample from a verified unedited T11 colony copy. No sprint."""
import argparse, copy, datetime, hashlib, json, os, pathlib, re, subprocess, tempfile, time, zipfile
import xml.etree.ElementTree as ET
import importlib.util
_spec=importlib.util.spec_from_file_location('restart_verification',pathlib.Path(__file__).with_name('restart-verification.py'))
_restart=importlib.util.module_from_spec(_spec);_spec.loader.exec_module(_restart)
def inputs():
 manifest=_restart.inputs()
 for name in ['gradlew','gradlew.bat']:
  f=pathlib.Path(__file__).resolve().parents[1]/name
  manifest[name]=hashlib.sha256(f.read_bytes()).hexdigest()
 for f in (ROOT/'config').rglob('*'):
  if f.is_file():manifest[str(f.relative_to(ROOT))]=sha(f)
 return manifest

ROOT=pathlib.Path(__file__).resolve().parents[1]
TURNLOOP=pathlib.Path(os.environ.get('TURNLOOP_HOME') or pathlib.Path(__file__).resolve().parents[2]/'turnloop')
SOURCE=TURNLOOP/'directions/prime-ants-slice1/turns/T11/t11-a4-world.zip'
EXPECTED='d88c675faca2ca162c172445636c30b718e4b43ef5d0722719f729477993411f'
FRESH_TASKS=['test','runFastGameTests','runLongGameTests','verifyAntModel','verifyNestCacheAssets','verifyMaterialStoreAssets','verifyPublicPaths','testPublicPaths','testClientEvidenceValidator','testGameTestHarness','testServerWorldRetention','verifyShardedGameTests']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2),encoding='utf-8')
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def source_only(manifest):return {k:v for k,v in manifest.items() if not k.startswith('build/') and not k.startswith('build\\')}
def required_cases():
 spec=importlib.util.spec_from_file_location('gametest_harness',ROOT/'scripts/gametest-harness.py')
 harness=importlib.util.module_from_spec(spec);spec.loader.exec_module(harness)
 return harness.unit_cases(),set(harness.discover()),harness.model_cases()

def record_acceptance(evidence,name,phase):
 manifest=inputs();target=evidence/(name+'-inputs-'+phase+'.json')
 if target.exists():raise ValueError('Immutable input snapshot already exists')
 record={'inputs':manifest}
 if phase=='after':
  before=read(evidence/(name+'-inputs-before.json'))['inputs']
  record['source_unchanged']=source_only(before)==source_only(manifest)
  folder=evidence/(name+'-executed');folder.mkdir()
  files=list((ROOT/'build/test-results/test').glob('TEST-*.xml'))+[ROOT/'build/test-results/gametest/server.xml',ROOT/'build/test-results/client-model/headless.xml']+list((ROOT/'build/libs').glob('*.jar'))
  record['artifacts']={}
  for f in files:
   if not f.exists():continue
   dest=folder/f.name;dest.write_bytes(f.read_bytes())
   record['artifacts'][f.name]={'path':str(dest),'sha256':sha(dest),'executed_mtime':f.stat().st_mtime,'source':str(f)}
  meta=read(evidence/(name+'.json'));start=datetime.datetime.fromisoformat(meta['start']).timestamp()
  shard_root=pathlib.Path(os.environ.get('PRIME_ANTS_GAMETEST_EVIDENCE',ROOT/'build/test-results/gametest/runs'))
  runs=[p.parent for p in shard_root.glob('*/frozen.json') if p.stat().st_mtime>=start]
  if len(runs)!=1:raise ValueError('Exactly one frozen complete invocation required for archive')
  record['sharded_run']=str(runs[0]);record['sharded_archive']=str(folder/'sharded'/runs[0].name)
  for f in runs[0].rglob('*'):
   if not f.is_file():continue
   key='sharded/'+runs[0].name+'/'+f.relative_to(runs[0]).as_posix();dest=folder/key;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(f.read_bytes())
   record['artifacts'][key]={'path':str(dest),'sha256':sha(dest),'executed_mtime':f.stat().st_mtime,'source':str(f)}
 write(target,record)

def acceptance_gate(evidence,name):
 meta=read(evidence/(name+'.json'))
 if meta.get('exit_code')!=0 or not meta.get('end') or not meta.get('elapsed_seconds',0)>0:raise ValueError('Completed passing acceptance required')
 if meta.get('command')!='.\\gradlew.bat build --console=plain --rerun-tasks':raise ValueError('Actual unfiltered rerun build command required')
 if pathlib.Path(meta['working_directory']).resolve()!=ROOT:raise ValueError('Wrong acceptance workspace')
 start=datetime.datetime.fromisoformat(meta['start']).timestamp();end=datetime.datetime.fromisoformat(meta['end']).timestamp()
 after=read(evidence/(name+'-inputs-after.json'));before=read(evidence/(name+'-inputs-before.json'))
 if not after.get('source_unchanged') or source_only(before['inputs'])!=source_only(after['inputs']) or after['inputs']!=inputs():raise ValueError('Stale or changed source/build inputs')
 b=pathlib.Path(meta['log']).read_bytes();log=b.decode('utf-16' if b.startswith(b'\xff\xfe') else 'utf-8-sig')
 if 'BUILD SUCCESSFUL' not in log:raise ValueError('Build completion missing')
 for task in FRESH_TASKS:
  if not re.search(r'^> Task :'+task+r'\s*$',log,re.M):raise ValueError('Fresh executed task required: '+task)
 artifacts=after['artifacts'];counts={}
 for kind,required,files in [('unit',required_cases()[0],[a for n,a in artifacts.items() if n.startswith('TEST-')]),('server',required_cases()[1],[artifacts.get('server.xml')]),('model',required_cases()[2],[artifacts.get('headless.xml')])]:
  seen=[]
  if not files or any(f is None for f in files):raise ValueError('Missing '+kind+' reports')
  for a in files:
   f=pathlib.Path(a['path'])
   if not f.exists() or sha(f)!=a['sha256'] or not start<=a['executed_mtime']<=end:raise ValueError('Missing, changed or stale '+kind+' report')
   root=ET.parse(f).getroot()
   if any(e.tag in ['failure','error','skipped'] for e in root.iter()):raise ValueError('Failed/error/skipped '+kind+' report')
   for suite in root.iter('testsuite'):
    if any(int(suite.get(k,'0')) for k in ['failures','errors','skipped']):raise ValueError('Nonpassing suite totals')
   cases=list(root.iter('testcase'))
   if kind=='server':seen.extend(c.get('name') for c in cases)
   else:seen.extend((c.get('classname'),c.get('name')) for c in cases)
  if len(seen)!=len(required) or set(seen)!=required:raise ValueError('Incomplete/duplicate required '+kind+' discovery')
  counts[kind]=len(seen)
 if not after.get('sharded_archive'):raise ValueError('Individual child archives required; singleton report is insufficient')
 if after.get('sharded_archive'):
  run=pathlib.Path(after['sharded_archive']);verification=read(run/'verification.json');frozen=read(run/'frozen.json')
  if not verification.get('passed') or verification.get('permanent_count')!=counts['server']:raise ValueError('Nonpassing/incomplete sharded verification')
  seen=[]
  for suite in ['fast','long']:
   summary=read(run/(suite+'.json'))
   if not summary['passed'] or (suite=='long' and summary['scope']!='complete long suite'):raise ValueError('Targeted/failed suite cannot settle acceptance')
   for child in summary['shards']:
    if child['exit_code']!=0 or not child['inputs_unchanged'] or not child['lease_released_after_exit']:raise ValueError('Invalid child process/lease/input evidence')
    child_folder=run/(suite+'-'+str(child['index']));process=read(child_folder/'process.json');execution=read(child_folder/'execution.json')
    if process!=child or execution['names']!=child['assigned'] or set(execution['permanent_names'])!=required_cases()[1] or execution['frozen_sha256']!=sha(run/'frozen.json'):raise ValueError('Child execution/process manifest differs from freeze')
    spec=importlib.util.spec_from_file_location('archive_harness',ROOT/'scripts/gametest-harness.py');harness=importlib.util.module_from_spec(spec);spec.loader.exec_module(harness)
    _,issues=harness.validate_report(child_folder/'server.xml',child['assigned'],child['start_epoch'],child['end_epoch'],child['exit_code'])
    if issues:raise ValueError('Archived child report invalid: '+str(issues))
    seen.extend(child['assigned'])
  if len(seen)!=counts['server'] or set(seen)!=required_cases()[1]:raise ValueError('Child assigned union differs')
  for key,a in artifacts.items():
   if key.startswith('sharded/') and sha(pathlib.Path(a['path']))!=a['sha256']:raise ValueError('Changed archived shard evidence')
 for n,a in artifacts.items():
  if n.endswith('.jar') and sha(pathlib.Path(a['path']))!=a['sha256']:raise ValueError('Archived production inputs changed')
 if len([n for n in artifacts if n.endswith('.jar')])!=2:raise ValueError('Production archives missing')
 return {'status':'accepted','counts':counts,'command':meta['command'],'inputs':len(after['inputs'])}

def test_gate(destination):
 destination.mkdir(parents=True,exist_ok=False);results=[]
 with tempfile.TemporaryDirectory(prefix='prime-ants-offline-gate-') as temp:
  e=pathlib.Path(temp);name='offline-synthetic';now=time.time();art={}
  for kind,required,file in zip(['unit','server','model'],required_cases(),['TEST-unit.xml','server.xml','headless.xml']):
   root=ET.Element('testsuite',tests=str(len(required)),failures='0',errors='0',skipped='0')
   for case in sorted(required):ET.SubElement(root,'testcase',{'name':case,'classname':'offline'} if kind=='server' else {'classname':case[0],'name':case[1]})
   f=e/file;ET.ElementTree(root).write(f);art[file]={'path':str(f),'sha256':sha(f),'executed_mtime':now}
  for n in ['production.jar','production-sources.jar']:
   f=e/n;f.write_bytes(b'offline gate fixture');art[n]={'path':str(f),'sha256':sha(f)}
  passing_log=''.join('> Task :'+task+'\n' for task in FRESH_TASKS)+'BUILD SUCCESSFUL\n'
  log=e/'log';log.write_text(passing_log,encoding='utf-8')
  meta={'command':'.\\gradlew.bat build --console=plain --rerun-tasks','working_directory':str(ROOT),'start':datetime.datetime.fromtimestamp(now-5,datetime.timezone.utc).isoformat(),'end':datetime.datetime.fromtimestamp(now+5,datetime.timezone.utc).isoformat(),'elapsed_seconds':10,'exit_code':0,'log':str(log)}
  manifest=inputs();before={'inputs':manifest};after={'inputs':manifest,'source_unchanged':True,'artifacts':art}
  archive=e/'sharded';archive.mkdir();rows=read(ROOT/'config/gametest-suites.json')['cases'];write(archive/'frozen.json',{'inventory':rows});after['sharded_archive']=str(archive)
  write(archive/'verification.json',{'passed':True,'permanent_count':len(required_cases()[1])})
  for suite in ['fast','long']:
   assigned=[r['name'] for r in rows if r['suite']==suite];folder=archive/(suite+'-0');folder.mkdir()
   record={'index':0,'assigned':assigned,'exit_code':0,'inputs_unchanged':True,'lease_released_after_exit':True,'start_epoch':now-4,'end_epoch':now+4}
   write(folder/'process.json',record);write(folder/'execution.json',{'names':assigned,'permanent_names':sorted(required_cases()[1]),'frozen_sha256':sha(archive/'frozen.json')})
   root=ET.Element('testsuite',tests=str(len(assigned)),failures='0',errors='0',skipped='0')
   for n in assigned:ET.SubElement(root,'testcase',name=n,classname='offline synthetic only')
   ET.ElementTree(root).write(folder/'server.xml');write(archive/(suite+'.json'),{'passed':True,'scope':'complete long suite' if suite=='long' else 'complete fast server suite','shards':[record]})
  for f in archive.rglob('*'):
   if f.is_file():art['sharded/'+f.relative_to(archive).as_posix()]={'path':str(f),'sha256':sha(f),'executed_mtime':f.stat().st_mtime}
  def run(label,mutate,reject=True):
   m=copy.deepcopy(meta);a=copy.deepcopy(after);mutate(m,a);write(e/(name+'.json'),m);write(e/(name+'-inputs-before.json'),before);write(e/(name+'-inputs-after.json'),a)
   try:answer=acceptance_gate(e,name)
   except (ValueError,FileNotFoundError) as ex:
    if not reject:raise
    results.append({'fixture':label,'result':'rejected','reason':str(ex)});return
   if reject:raise AssertionError('Gate accepted '+label)
   results.append({'fixture':label,'result':'accepted offline synthetic only','counts':answer['counts']})
  run('valid-offline-synthetic',lambda m,a:None,False)
  run('compilation-only-success',lambda m,a:m.update(command='.\\gradlew.bat classes'))
  run('filtered-acceptance',lambda m,a:m.update(command=m['command']+' -PprimeAntsServerDiagnosticFilter=x'))
  run('incomplete-execution',lambda m,a:m.pop('end'))
  run('failed-build-exit',lambda m,a:m.update(exit_code=1))
  run('singleton-report-under-complete-command',lambda m,a:a.pop('sharded_archive'))
  run('missing-report',lambda m,a:a['artifacts'].pop('server.xml'))
  run('stale-inputs',lambda m,a:a['inputs'].update({'build/classes/stale':'old'}))
  run('stale-report',lambda m,a:a['artifacts']['server.xml'].update(executed_mtime=0))
  log.write_text('> Task :compileJava\nBUILD SUCCESSFUL\n')
  run('compilation-only-log-under-build-command',lambda m,a:None)
  log.write_text(passing_log.replace('> Task :test\n','> Task :test UP-TO-DATE\n'),encoding='utf-8')
  run('unexecuted-unit-task',lambda m,a:None)
  log.write_text(passing_log,encoding='utf-8')
  for label,tag in [('failed-report','failure'),('error-report','error'),('skipped-report','skipped')]:
   f=e/'server.xml';original=f.read_bytes();r=ET.parse(f);ET.SubElement(next(r.iter('testcase')),tag);r.write(f)
   run(label,lambda m,a:a['artifacts']['server.xml'].update(sha256=sha(f)));f.write_bytes(original)
  f=e/'server.xml';r=ET.parse(f);r.getroot().remove(next(r.iter('testcase')));r.write(f)
  run('incomplete-required-discovery',lambda m,a:a['artifacts']['server.xml'].update(sha256=sha(f)))
 write(destination/'results.json',results);print(json.dumps(results,indent=2))

def main():
 p=argparse.ArgumentParser();p.add_argument('--attempt');p.add_argument('--evidence',required=True);p.add_argument('--acceptance-name',default='t13-build-final');p.add_argument('--record-phase',choices=['before','after']);p.add_argument('--self-test-gate',action='store_true');p.add_argument('--gate-only',action='store_true');a=p.parse_args()
 evidence=pathlib.Path(a.evidence).resolve()
 if a.record_phase:record_acceptance(evidence,a.acceptance_name,a.record_phase);return
 if a.self_test_gate:test_gate(evidence);return
 gate=acceptance_gate(evidence.parent if not a.gate_only else evidence,a.acceptance_name)
 if a.gate_only:print(json.dumps(gate,indent=2));return
 if not a.attempt:raise ValueError('Explicit attempt required')
 if not a.attempt.replace('-','').isalnum() or a.attempt!=a.attempt.lower():raise ValueError('Unsafe attempt')
 evidence=pathlib.Path(a.evidence).resolve();evidence.mkdir(parents=True,exist_ok=True)
 write(evidence/'acceptance-gate.json',gate)
 run=(ROOT/'build/run/t09-restart'/a.attempt).resolve();run.relative_to((ROOT/'build/run/t09-restart').resolve())
 if run.exists():raise ValueError('Preserve previous owned attempt; choose fresh name')
 if sha(SOURCE)!=EXPECTED:raise ValueError('Verified source archive changed')
 world=run/'world';world.mkdir(parents=True)
 with zipfile.ZipFile(SOURCE) as z:
  prefix=next(n[:-len('level.dat')] for n in z.namelist() if n.endswith('/level.dat'))
  for n in z.namelist():
   if not n.startswith(prefix) or n.endswith('/'):continue
   dst=(world/n[len(prefix):]).resolve();dst.relative_to(world.resolve());dst.parent.mkdir(parents=True,exist_ok=True);dst.write_bytes(z.read(n))
 if not (world/'level.dat').exists():raise ValueError('Genuine saved world missing')
 source_files={str(f.relative_to(world)):sha(f) for f in world.rglob('*') if f.is_file()};write(evidence/'copied-world-before.json',source_files)
 (run/'eula.txt').write_text('eula=true\n',encoding='utf-8')
 (run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\npause-when-empty-seconds=0\nenable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\nmax-players=1\nmotd=T13 scoped 20 TPS measurement\n',encoding='utf-8')
 paths=list((ROOT/'src').rglob('*'))+list((ROOT/'gradle').rglob('*'))+list((ROOT/'build/classes').rglob('*'))+list((ROOT/'build/resources').rglob('*'))+list((ROOT/'build/libs').glob('*.jar'))
 frozen={str(f.relative_to(ROOT)):sha(f) for f in paths if f.is_file()};write(evidence/'frozen-inputs.json',frozen)
 args=['runRestartServer','--console=plain',f'-PprimeAntsRestartAttempt={a.attempt}','-PprimeAntsRestartPhase=performance',f'-PprimeAntsRestartEvidence={evidence}','-PprimeAntsPerformanceQueen=0915c15f-8000-482e-8ccf-fc6caba553e1']
 quote=lambda s:"'"+str(s).replace("'","''")+"'"
 command=evidence/'command.ps1';command.write_text('& '+quote(ROOT/'scripts/Invoke-GradleEvidence.ps1')+' -Name performance-server -GradleArgs @('+','.join(map(quote,args))+') -EvidenceDirectory '+quote(evidence)+'\n$code=$LASTEXITCODE\nexit $code\n',encoding='utf-8')
 start=time.monotonic()
 with (evidence/'console.log').open('wb') as log:result=subprocess.run(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(command)],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
 record={'source_archive':str(SOURCE),'source_sha256':EXPECTED,'copy_root':prefix,'owned_world':str(world),'command_script':str(command),'gradle_args':args,'exit_code':result.returncode,'elapsed_seconds':time.monotonic()-start,'source_preserved':sha(SOURCE)==EXPECTED,'frozen_inputs_unchanged':all(f.exists() and sha(f)==v for k,v in frozen.items() for f in [ROOT/k])}
 with zipfile.ZipFile(evidence/'performance-world.zip','w',zipfile.ZIP_DEFLATED) as z:
  for f in world.rglob('*'):
   if f.is_file():z.write(f,f.relative_to(world))
 write(evidence/'verification.json',record)
 if result.returncode or (evidence/'performance-failure.json').exists() or not (evidence/'performance.json').exists():raise RuntimeError('Sample failed; inspect preserved command/error/world before retry')
 measured=json.loads((evidence/'performance.json').read_text());rows=measured['samples'];record.update({k:measured[k] for k in ['median_mspt','p95_mspt','max_mspt','sample_ticks','warmup_loaded_ticks']})
 record.update(workers_range=[min(r['workers'] for r in rows),max(r['workers'] for r in rows)],queens_range=[min(r['queens'] for r in rows),max(r['queens'] for r in rows)],loaded_chunks_range=[min(r['loaded_chunks'] for r in rows),max(r['loaded_chunks'] for r in rows)],food_carrying_ticks=sum(r['carried_food']>0 for r in rows),consumed_units_delta=rows[-1]['consumed_food']-rows[0]['consumed_food'])
 assert len(rows)==1200 and all(r['tickrate']==20 and not r['sprint'] for r in rows)
 assert all(b['tick']==a['tick']+1 and b['queen_age']==a['queen_age']+1 for a,b in zip(rows,rows[1:]))
 assert record['source_preserved'] and record['frozen_inputs_unchanged']
 write(evidence/'verification.json',record);print(json.dumps(record,indent=2))
if __name__=='__main__':main()
