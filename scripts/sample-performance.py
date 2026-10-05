"""One development 20 TPS sample from a verified unedited T11 colony copy. No sprint."""
import argparse, hashlib, json, pathlib, subprocess, time, zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
SOURCE=pathlib.Path(r'C:/Users/user/Documents/turnloop/directions/prime-ants-slice1/turns/T11/t11-a4-world.zip')
EXPECTED='d88c675faca2ca162c172445636c30b718e4b43ef5d0722719f729477993411f'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2),encoding='utf-8')
def main():
 p=argparse.ArgumentParser();p.add_argument('--attempt',required=True);p.add_argument('--evidence',required=True);p.add_argument('--acceptance-name',default='t12-build-final-recovered');a=p.parse_args()
 if not a.attempt.replace('-','').isalnum() or a.attempt!=a.attempt.lower():raise ValueError('Unsafe attempt')
 evidence=pathlib.Path(a.evidence).resolve();evidence.mkdir(parents=True,exist_ok=True)
 acceptance=json.loads((evidence.parent/(a.acceptance_name+'.json')).read_text(encoding='utf-8-sig'))
 if acceptance.get('exit_code')!=0:raise ValueError('Completed green unfiltered acceptance required before measurement')
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
 (run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\npause-when-empty-seconds=0\nenable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\nmax-players=1\nmotd=T12 scoped 20 TPS measurement\n',encoding='utf-8')
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
