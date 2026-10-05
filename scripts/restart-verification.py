"""Development-only copy/save/exit/reopen check. Each invocation owns a new attempt; never erases old worlds."""
import argparse, datetime, hashlib, json, pathlib, subprocess, sys, time, zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
SOURCE=pathlib.Path(r'C:\Users\user\Documents\turnloop\directions\prime-ants-slice1\turns\T08\46-a4-world.zip')
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,v): p.write_text(json.dumps(v,indent=2),encoding='utf-8')
def inputs():
 paths=list((ROOT/'src').rglob('*'))+list((ROOT/'gradle').rglob('*'))+list((ROOT/'scripts').rglob('*'))+[ROOT/'build.gradle',ROOT/'gradle.properties',ROOT/'settings.gradle']
 paths+=list((ROOT/'build/classes').rglob('*'))+list((ROOT/'build/resources').rglob('*'))+list((ROOT/'build/libs').glob('*.jar'))
 return {str(p.relative_to(ROOT)):sha(p) for p in sorted(set(paths)) if p.is_file() and '__pycache__' not in str(p)}
def tree(path): return {str(p.relative_to(path)):sha(p) for p in path.rglob('*') if p.is_file()}
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--attempt',required=True);parser.add_argument('--evidence',required=True);parser.add_argument('--probe',action='store_true');parser.add_argument('--phase-only',choices=['geometry','active']);a=parser.parse_args()
 if not a.attempt.replace('-','').isalnum() or a.attempt.lower()!=a.attempt: raise ValueError('Unsafe attempt')
 owned=(ROOT/'build/run/t09-restart').resolve();run=(owned/a.attempt).resolve();run.relative_to(owned)
 if run.exists(): raise ValueError('Attempt already exists; preserve it and choose a fresh name')
 evidence=pathlib.Path(a.evidence).resolve();evidence.mkdir(parents=True,exist_ok=True);run.mkdir(parents=True)
 world=run/'world';world.mkdir()
 source_sha=sha(SOURCE)
 with zipfile.ZipFile(SOURCE) as z:
  for n in z.namelist():
   target=(world/n).resolve();target.relative_to(world.resolve())
   if n.endswith('/'):target.mkdir(parents=True,exist_ok=True)
   else:target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(n))
 if not (world/'level.dat').exists():raise ValueError('Copied archive is not a real world')
 (run/'eula.txt').write_text('eula=true\n',encoding='utf-8')
 (run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\npause-when-empty-seconds=0\nenable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\nmax-players=1\nmotd=T09 scoped development restart\n',encoding='utf-8')
 frozen=inputs();write(evidence/'frozen-inputs.json',frozen)
 result={'attempt':a.attempt,'world_path':str(world),'source_archive':str(SOURCE),'source_sha256':source_sha,'source_preserved':False,'processes':[],'scope':'Normal flushed graceful checkpoints only; no abrupt-crash consistency claim.'}
 write(evidence/'verification.json',result)
 for phase in ([a.phase_only] if a.phase_only else ['probe'] if a.probe else ['A','B']):
  before=tree(world);write(evidence/(phase+'-world-before.json'),before)
  cmd=['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(ROOT/'scripts/Invoke-GradleEvidence.ps1'),'-Name',phase+'-server','-GradleArgs','runRestartServer',f'-PprimeAntsRestartAttempt={a.attempt}',f'-PprimeAntsRestartPhase={phase}',f'-PprimeAntsRestartEvidence={evidence}','--console=plain','-EvidenceDirectory',str(evidence)]
  # -File cannot bind multiple native positional arguments to string[] reliably; use a separate exact script.
  ps=evidence/(phase+'-command.ps1')
  def q(s):return "'"+str(s).replace("'","''")+"'"
  args=['runRestartServer',f'-PprimeAntsRestartAttempt={a.attempt}',f'-PprimeAntsRestartPhase={phase}',f'-PprimeAntsRestartEvidence={evidence}','--console=plain']
  ps.write_text("& "+q(ROOT/'scripts/Invoke-GradleEvidence.ps1')+" -Name "+q(phase+'-server')+" -GradleArgs @("+','.join(map(q,args))+") -EvidenceDirectory "+q(evidence)+"\n$code = $LASTEXITCODE\nexit $code\n",encoding='utf-8')
  cmd=['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(ps)]
  print('START '+phase+' '+str(ps),flush=True);start=time.monotonic()
  with (evidence/(phase+'-console.log')).open('wb') as log:cp=subprocess.run(cmd,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
  record={'phase':phase,'command':cmd,'exit_code':cp.returncode,'elapsed_seconds':time.monotonic()-start}
  result['processes'].append(record);write(evidence/'verification.json',result)
  if (evidence/(phase+'-process.json')).exists():record.update(read(evidence/(phase+'-process.json')))
  record['gradle_command']=read(evidence/(phase+'-server.json'))['command'];write(evidence/'verification.json',result)
  if cp.returncode:raise RuntimeError(f'{phase} Gradle/native JVM failure; see {evidence}')
  native=read(evidence/(phase+'-process.json'))
  check=subprocess.run(['powershell.exe','-NoProfile','-Command',f'$p = Get-Process -Id {native["pid"]} -ErrorAction SilentlyContinue; if ($null -ne $p) {{ exit 1 }}; exit 0'],capture_output=True)
  record['confirmed_exited']=check.returncode==0
  record['normal_stop_callback']=(evidence/(phase+'-stopped.json')).exists();write(evidence/'verification.json',result)
  if check.returncode:raise RuntimeError('Task JVM has not exited; B must not start')
  if not (evidence/(phase+'-stopped.json')).exists():raise RuntimeError('Normal server stop callback missing')
  if (evidence/(phase+'-failure.json')).exists():raise RuntimeError('Harness assertion failed: '+read(evidence/(phase+'-failure.json'))['failure'])
  terminal=phase+'-checkpoint' if phase=='A' else phase+'-recovered' if phase=='B' else phase
  if not (evidence/(terminal+'.json')).exists():raise RuntimeError('Required checkpoint missing')
  record['checkpoint']=read(evidence/(terminal+'.json'))['checkpoint']
  after=tree(world);write(evidence/(phase+'-world-after.json'),after)
  if phase=='B':
   if before!=read(evidence/'A-world-after.json'):raise RuntimeError('Saved world edited between A and B')
   if native['world']!=result['processes'][0]['world']:raise RuntimeError('B did not open the same saved world')
   if native['pid']==result['processes'][0]['pid'] or native['started']==result['processes'][0]['started']:raise RuntimeError('Not separate JVM identities')
  with zipfile.ZipFile(evidence/(phase+'-world.zip'),'w',zipfile.ZIP_DEFLATED) as z:
   for p in world.rglob('*'):
    if p.is_file():z.write(p,p.relative_to(world))
  # DLI/launch configs may change, but production and compiled source inputs stay frozen.
  if inputs()!=frozen:raise RuntimeError('Frozen source/build inputs changed during restart; diagnose before new final attempt')
  write(evidence/'verification.json',result);print('EXIT '+phase+' pid='+str(native['pid'])+' code=0',flush=True)
 result['source_preserved']=sha(SOURCE)==source_sha
 if not result['source_preserved']:raise RuntimeError('Original archive changed')
 if not a.probe and not a.phase_only:
  result['before']=read(evidence/'A-checkpoint.json');result['home_first']=read(evidence/'B-home-first.json');result['restored']=read(evidence/'B-restored.json');result['after']=read(evidence/'B-recovered.json');result['recovered']=result['after']['recovered'];result['same_unedited_world']=True
 write(evidence/'verification.json',result)
 print('VERIFIED '+str(evidence/'verification.json'),flush=True)
if __name__=='__main__':
 try:main()
 except Exception as e:print('FAILURE '+str(e),flush=True);sys.exit(1)
