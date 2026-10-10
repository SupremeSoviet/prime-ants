"""Exact permanent inventory and isolated, bounded GameTest JVMs. Never invokes Gradle."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import copy
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import uuid
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('retention', Path(__file__).with_name('retain-server-worlds.py'))
retention = importlib.util.module_from_spec(spec); spec.loader.exec_module(retention)

def read(p): return json.loads(Path(p).read_text(encoding='utf-8-sig'))
def write(p, value): Path(p).write_text(json.dumps(value, indent=2, sort_keys=True), encoding='utf-8')
def sha(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def snake(s): return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s).lower()

def discover(root=ROOT):
    registrations = read(root/'src/gametest/resources/fabric.mod.json')['entrypoints']['fabric-gametest']
    if len(registrations) != len(set(registrations)): raise ValueError('Duplicate registered class')
    cases = {}
    for name in registrations:
        path = root/('src/gametest/java/'+name.replace('.', '/')+'.java')
        source = path.read_text(encoding='utf-8')
        matches = re.findall(r'@GameTest(?:\(([^)]*)\))?\s+public void (\w+)\(GameTestHelper \w+\)', source)
        if len(matches) != len(re.findall(r'@GameTest\b',source)): raise ValueError('Unparsed permanent annotations: '+name)
        for annotation, method in matches:
            if re.search(r'manualOnly\s*=\s*true',annotation): raise ValueError('Permanent manual-only case')
            tick = re.search(r'maxTicks\s*=\s*(\d+)',annotation)
            identity = 'prime_ants_test:'+snake(name.split('.')[-1])+'_'+snake(method)
            if identity in cases: raise ValueError('Duplicate permanent identity: '+identity)
            cases[identity] = {'name':identity, 'max_ticks':int(tick[1]) if tick else 20, 'source':str(path.relative_to(root))}
    if not cases: raise ValueError('No permanent cases')
    return cases

def inventory(root=ROOT):
    declared = discover(root); rows = read(root/'config/gametest-suites.json')['cases']
    names = [r['name'] for r in rows]
    if len(names)!=len(set(names)) or set(names)!=set(declared):
        raise ValueError('Omitted/duplicated/unknown inventory identities: '+str(sorted(set(names)^set(declared))))
    for row in rows:
        if row.get('suite') not in ('fast','long') or not row.get('reason') or row.get('weight_ticks',0)<=0:
            raise ValueError('Unclassified/invalid permanent identity: '+row['name'])
        if row['max_ticks'] != declared[row['name']]['max_ticks']: raise ValueError('Changed declared tick path: '+row['name'])
    return rows

def unit_cases():
    rows=[]
    for path in (ROOT/'src/test/java').rglob('*.java'):
        source=path.read_text(encoding='utf-8');package=re.search(r'package ([\w.]+);',source)[1];cls=package+'.'+path.stem
        methods=re.findall(r'@Test\s+(?:public\s+)?void (\w+)\(',source)
        if len(methods)!=len(re.findall(r'@Test\b',source)):raise ValueError('Unclassified unit methods: '+str(path))
        rows.extend((cls,m+'()') for m in methods)
        params=re.findall(r'@ParameterizedTest\(name\s*=\s*"([^"]+)"\)\s+@(EnumSource|MethodSource)\(([^)]*)\)\s+void \w+\(',source)
        if len(params)!=source.count('@ParameterizedTest'):raise ValueError('Unclassified unit parameterization: '+str(path))
        for label,provider,body in params:
            if provider=='EnumSource':
                if 'ColonyStage.class' not in body:raise ValueError('Unclassified enum provider')
                values=re.findall(r'"(\w+)"',body) or ['FOUNDING','YOUNG','MATURE','GREAT']
            else:
                method=body.strip('"');values=re.search(r'static DoubleStream '+method+r'\(\)\s*\{\s*return DoubleStream.of\(([^)]*)\)',source)[1].split(',')
                symbolic={'Double.NaN':'NaN','Double.POSITIVE_INFINITY':'Infinity','Double.NEGATIVE_INFINITY':'-Infinity','Double.MAX_VALUE':'1.7976931348623157E308'}
                values=[symbolic[v.strip()] if v.strip() in symbolic else str(float(v.strip())) for v in values]
            rows.extend((cls,label.replace('{0}',v)) for v in values)
    if len(rows)!=len(set(rows)) or not rows:raise ValueError('Duplicate/empty unit identities')
    return set(rows)

def model_cases():
    source=(ROOT/'src/gametest/java/dev/primeants/gametest/AntModelGameTest.java').read_text(encoding='utf-8')
    names=set(re.findall(r'test\(form, "(\w+)"',source))-{'profilesOnly'}
    return {('AntModel.'+form,name) for form in ['WORKER','QUEEN'] for name in names}

def balance(rows, count):
    if count < 1: raise ValueError('Positive shard count required')
    bins=[[] for _ in range(min(count,len(rows)))]; weights=[0]*len(bins)
    for row in sorted(rows,key=lambda r:(-r['weight_ticks'],r['name'])):
        i=min(range(len(bins)),key=lambda i:(weights[i],i));bins[i].append(row['name']);weights[i]+=row['weight_ticks']
    return [{'index':i,'names':sorted(names),'weight_ticks':weights[i]} for i,names in enumerate(bins)]

def inputs(launch):
    paths=[]
    for base in ['src','scripts','gradle','config','build/classes','build/resources']:
        paths += [p for p in (ROOT/base).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
    paths += [ROOT/n for n in ['build.gradle','gradle.properties','settings.gradle','gradlew','gradlew.bat','.gradle/loom-cache/launch.cfg']]
    paths += [Path(p) for p in launch['classpath'] if Path(p).is_file()]
    return {str(p.resolve()):sha(p) for p in sorted(set(paths))}

def prepare(folder, launch_path, shards, concurrency, names=None):
    if not 1<=concurrency<=4: raise ValueError('Concurrency must be 1..4 total servers')
    folder=Path(folder);folder.mkdir(parents=True,exist_ok=False)
    launch=read(launch_path); rows=inventory()
    long=[r for r in rows if r['suite']=='long']
    if names:
        selected=names.split(',')
        if len(selected)!=len(set(selected)) or not set(selected)<=set(r['name'] for r in long): raise ValueError('Exact long names must be unique registered long cases')
        long=[r for r in long if r['name'] in selected]
    assignment={'fast':balance([r for r in rows if r['suite']=='fast'],1), 'long':balance(long,shards)}
    freeze={'launch':launch,'inputs':inputs(launch),'inventory':rows,'assignments':assignment,'concurrency':concurrency,
            'unit_cases':sorted(unit_cases()),'model_cases':sorted(model_cases()),
            'long_scope':'targeted exact names' if names else 'complete long suite','prepared_epoch':time.time()}
    write(folder/'frozen.json',freeze)
    print(f"Frozen permanent partition: {len(rows)} cases; fast={sum(r['suite']=='fast' for r in rows)} long={sum(r['suite']=='long' for r in rows)}; concurrency={concurrency}",flush=True)

def validate_report(path, assigned, start, end, exit_code):
    issues=[]; cases=[]; path=Path(path)
    if len(assigned)!=len(set(assigned)): issues.append('duplicate assignment')
    if exit_code!=0: issues.append('failed child exit: '+str(exit_code))
    try:
        if not start<=path.stat().st_mtime<=end: issues.append('stale report')
        root=ET.parse(path).getroot()
        if root.tag not in ('testsuite','testsuites'): issues.append('malformed report root')
        cases=list(root.iter('testcase')); actual=[c.get('name') for c in cases]
        if len(actual)!=len(set(actual)): issues.append('duplicate executed names')
        if set(actual)!=set(assigned) or len(actual)!=len(assigned): issues.append('missing/unexpected executed names: '+str(sorted(set(actual)^set(assigned))))
        for suite in root.iter('testsuite'):
            if 'tests' in suite.attrib and int(suite.get('tests'))!=len(list(suite.iter('testcase'))): issues.append('incomplete XML totals')
            for key in ('failures','errors','skipped'):
                if int(suite.get(key,'0')): issues.append('nonzero '+key)
        for key in ('failure','error','skipped'):
            if any(True for _ in root.iter(key)): issues.append('report '+key)
    except (OSError,ET.ParseError,ValueError) as error:
        issues.append('missing/malformed report: '+str(error))
    return cases,issues

def merged(path, cases):
    # Preserve every row and failure. Duplicates are rejected, never hidden by a set.
    root=ET.Element('testsuite',name='Prime Ants merged server evidence',tests=str(len(cases)),
        failures=str(sum(any(True for _ in c.iter('failure')) for c in cases)),errors=str(sum(any(True for _ in c.iter('error')) for c in cases)),skipped=str(sum(any(True for _ in c.iter('skipped')) for c in cases)))
    for case in sorted(cases,key=lambda c:c.get('name','')): root.append(copy.deepcopy(case))
    ET.ElementTree(root).write(path,encoding='utf-8',xml_declaration=True)

def written_report_end(path):
    # NTFS and Python epoch doubles can differ by one 0.238-us ULP immediately after write.
    # Capture a later clock reading; validation still uses the unchanged strict interval.
    modified=Path(path).stat().st_mtime;end=time.time()
    if 0<modified-end<=0.001:
        time.sleep(0.001);end=time.time()
    return end

def ps_quote(s): return "'"+str(s).replace("'","''")+"'"
def java_quote(s): return '"'+str(s).replace('\\','\\\\').replace('"','\\"')+'"'

def child(folder, suite, assignment, freeze):
    folder=Path(folder);folder.mkdir();run_id=str(uuid.uuid4());start=time.time();timer=time.monotonic()
    record={'index':assignment['index'],'assigned':assignment['names'],'weight_ticks':assignment['weight_ticks'],'world_uuid':run_id,'start_epoch':start,'exit_code':None}
    lease=None;process=None
    try:
        if inputs(freeze['launch'])!=freeze['inputs']: raise ValueError('Frozen inputs changed before launch')
        run,lease,retained=retention.register(ROOT/'build/run/serverGameTest',run_id)
        record.update(world=str(run),lease=str(run.parent/'.leases'/(run_id+'.lock')),retention=retained)
        (run/'eula.txt').write_text('eula=true\n',encoding='utf-8')
        # Same vanilla GameTest environment/properties. No simulation, terrain, heap or clock changes.
        manifest=folder/'execution.json';write(manifest,{'label':suite,'names':assignment['names'],'permanent_names':[r['name'] for r in freeze['inventory']],'world_uuid':run_id,'frozen_sha256':sha(folder.parent/'frozen.json')})
        report=folder/'server.xml';launch=freeze['launch'];args=[]
        for arg in launch['jvm_args']:
            if arg.startswith(('-Dfabric-api.gametest.report-file=','-Dfabric-api.gametest.filter=')): continue
            args.append(arg)
        args += ['-Dfabric-api.gametest.report-file='+str(report),'-Dfabric-api.gametest.filter=prime_ants_test:*','-Dprime_ants.caseManifest='+str(manifest),'-classpath',os.pathsep.join(launch['classpath']),launch['main_class'],*launch['args']]
        argfile=folder/'launch.args';argfile.write_text('\n'.join(java_quote(a) for a in args)+'\n',encoding='utf-8')
        script=folder/'launch.ps1';stdout=folder/'server.log';stderr=folder/'stderr.log'
        script.write_text("$ErrorActionPreference='Stop'\n$p=Start-Process -FilePath "+ps_quote(launch['java'])+" -ArgumentList "+ps_quote('@"'+str(argfile)+'"')+" -WorkingDirectory "+ps_quote(run)+" -RedirectStandardOutput "+ps_quote(stdout)+" -RedirectStandardError "+ps_quote(stderr)+" -WindowStyle Hidden -PassThru\n$handle=$p.Handle\n[IO.File]::WriteAllText("+ps_quote(folder/'pid.json')+",('{\"pid\":'+$p.Id+'}'),[Text.UTF8Encoding]::new($false))\n$p.WaitForExit()\nexit $p.ExitCode\n",encoding='utf-8')
        command=['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(script)]
        record['command']=command;write(folder/'process.json',record)
        print(f"START {suite} shard={assignment['index']} assigned={len(assignment['names'])} world={run_id}",flush=True)
        with (folder/'launcher.log').open('wb') as log:
            process=subprocess.Popen(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
            record['launcher_pid']=process.pid;write(folder/'process.json',record);record['exit_code']=process.wait()
        record['end_epoch']=time.time();record['elapsed_seconds']=time.monotonic()-timer
        if (folder/'pid.json').exists():
            record['java_pid']=read(folder/'pid.json')['pid'];record['native_start_epoch']=(folder/'pid.json').stat().st_mtime
            record['native_wall_seconds']=record['end_epoch']-record['native_start_epoch']
        cases,issues=validate_report(report,assignment['names'],start,record['end_epoch'],record['exit_code'])
        text=stdout.read_text(encoding='utf-8-sig') if stdout.exists() else ''
        if 'GAME TESTS COMPLETE' not in text or 'Game test server shutting down' not in text: issues.append('complete execution/shutdown markers missing')
        record['inputs_unchanged']=inputs(launch)==freeze['inputs']
        if not record['inputs_unchanged']: issues.append('compiled/source inputs changed during child')
        record.update(issues=issues,executed=len(cases),report_sha256=sha(report) if report.exists() else None)
        record['passed']=not issues
    except Exception as error:
        record.update(passed=False,issues=['child infrastructure: '+str(error)],end_epoch=time.time(),elapsed_seconds=time.monotonic()-timer)
    finally:
        # Parent waits for the launcher and native JVM before releasing the active-world lease.
        if process is not None and process.poll() is None:
            record['exit_code']=process.wait()
        retention.close_handle(lease);record['lease_released_after_exit']=record['exit_code'] is not None
        write(folder/'process.json',record)
    print(f"END {suite} shard={assignment['index']} exit={record['exit_code']} seconds={record['elapsed_seconds']:.3f} passed={record['passed']}",flush=True)
    return record

def run_suite(folder, suite):
    folder=Path(folder);freeze=read(folder/'frozen.json');start=time.time();timer=time.monotonic()
    assignments=freeze['assignments'][suite] if suite!='smoke' else balance([r for r in freeze['inventory'] if r['name'] in (
        'prime_ants_test:bootstrap_game_test_food_item_lifecycle','prime_ants_test:surface_oracle_game_test_off_footprint_player_and_unevidenced_mod_edits_are_rejected')],2)
    with ThreadPoolExecutor(max_workers=freeze['concurrency'] if suite!='fast' else 1) as pool:
        futures=[pool.submit(child,folder/(suite+'-'+str(a['index'])),suite,a,freeze) for a in assignments]
        records=[f.result() for f in futures]
    cases=[]
    for r in records:
        path=folder/(suite+'-'+str(r['index']))/'server.xml'
        try: cases.extend(ET.parse(path).getroot().iter('testcase'))
        except (OSError,ET.ParseError): pass
    report=folder/(suite+'.xml');merged(report,cases)
    assigned=[n for a in assignments for n in a['names']]
    _,union=validate_report(report,assigned,start,written_report_end(report),0)
    native=[r for r in records if 'native_start_epoch' in r]
    peak=max((sum(r['native_start_epoch']<=t<r['end_epoch'] for r in native) for t in [r['native_start_epoch'] for r in native]),default=0)
    summary={'suite':suite,'scope':freeze['long_scope'] if suite=='long' else ('multi-process smoke only' if suite=='smoke' else 'complete fast server suite'),
        'start_epoch':start,'end_epoch':time.time(),'elapsed_seconds':time.monotonic()-timer,'shards':records,'assigned':assigned,
        'union_issues':union,'peak_concurrent_launches':peak,'passed':all(r['passed'] for r in records) and not union}
    write(folder/(suite+'.json'),summary)
    return summary

def merge_complete(folder):
    folder=Path(folder);freeze=read(folder/'frozen.json');cases=[];issues=[];summaries={}
    if freeze['long_scope']!='complete long suite': issues.append('Targeted names cannot be complete acceptance')
    for suite in ('fast','long'):
        summaries[suite]=read(folder/(suite+'.json'))
        if not summaries[suite]['passed']: issues.append(suite+' suite failed')
        cases.extend(ET.parse(folder/(suite+'.xml')).getroot().iter('testcase'))
    report=folder/'server.xml';merged(report,cases)
    _,union=validate_report(report,[r['name'] for r in freeze['inventory']],freeze['prepared_epoch'],written_report_end(report),0);issues+=union
    if inputs(freeze['launch'])!=freeze['inputs']: issues.append('Frozen inputs changed')
    result={'passed':not issues,'issues':issues,'permanent_count':len(freeze['inventory']),'executed':len(cases),'suites':summaries,'merged_sha256':sha(report),'frozen_sha256':sha(folder/'frozen.json')}
    write(folder/'verification.json',result)
    destination=ROOT/'build/test-results/gametest/server.xml';destination.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(report,destination)
    return result

def fast_guards(folder, since):
    folder=Path(folder);freeze=read(folder/'frozen.json');result={'passed':True,'issues':[],'reports':{}}
    for kind,paths,required in [('unit',list((ROOT/'build/test-results/test').glob('TEST-*.xml')),freeze['unit_cases']),
            ('model',[ROOT/'build/test-results/client-model/headless.xml'],freeze['model_cases'])]:
        seen=[]
        for path in paths:
            try:
                if not since<=path.stat().st_mtime<=time.time():raise ValueError('Stale '+kind+' report')
                root=ET.parse(path).getroot()
                if any(e.tag in ('failure','error','skipped') for e in root.iter()):raise ValueError('Nonpassing '+kind+' report')
                for suite in root.iter('testsuite'):
                    if any(int(suite.get(k,'0')) for k in ('failures','errors','skipped')):raise ValueError('Nonzero '+kind+' totals')
                    if int(suite.get('tests','0'))!=len(list(suite.iter('testcase'))):raise ValueError('Incomplete '+kind+' XML totals')
                seen.extend((c.get('classname'),c.get('name')) for c in root.iter('testcase'))
                result['reports'][str(path.relative_to(ROOT))]={'sha256':sha(path),'mtime':path.stat().st_mtime}
            except (OSError,ET.ParseError,ValueError) as error:result['issues'].append(str(error))
        required={tuple(r) for r in required}
        if len(seen)!=len(required) or set(seen)!=required:result['issues'].append('Incomplete/duplicate '+kind+' permanent discovery')
        result[kind+'_count']=len(seen)
    result['passed']=not result['issues'];write(folder/'fast-guards.json',result);return result

def main():
    p=argparse.ArgumentParser();p.add_argument('action',choices=['inventory','prepare','run','merge','fast-guards']);p.add_argument('--folder',type=Path);p.add_argument('--launch',type=Path);p.add_argument('--suite',choices=['fast','long','smoke']);p.add_argument('--shards',type=int,default=4);p.add_argument('--concurrency',type=int,default=4);p.add_argument('--names');p.add_argument('--since',type=float);p.add_argument('--defer-failures',action='store_true');a=p.parse_args()
    if a.action=='inventory': print('Verified exact permanent partition:',len(inventory()),'unit:',len(unit_cases()),'model:',len(model_cases()));return
    if a.action=='prepare': prepare(a.folder,a.launch,a.shards,a.concurrency,a.names);return
    result=run_suite(a.folder,a.suite) if a.action=='run' else fast_guards(a.folder,a.since) if a.action=='fast-guards' else merge_complete(a.folder)
    print(json.dumps({k:v for k,v in result.items() if k not in ('suites','shards','assigned')},indent=2),flush=True)
    if not result['passed'] and not a.defer_failures: raise SystemExit(1)

if __name__=='__main__': main()
