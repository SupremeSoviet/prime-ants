"""Opt-in T10 owned normal server runner. No historical world extraction or save editing."""
import argparse, hashlib, importlib.util, json, re, shutil, subprocess, time, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2),encoding='utf-8')
def inputs():
    files=[p for folder in ('src','scripts','gradle','config','build/classes','build/resources') for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
    files += [ROOT/x for x in ('build.gradle','gradle.properties','settings.gradle')]
    return {p.relative_to(ROOT).as_posix():sha(p) for p in files}
def main():
    p=argparse.ArgumentParser();p.add_argument('--attempt',required=True);p.add_argument('--phase',required=True,choices=['probe','profile','control','empty','one','three','prepare','reopen1','reopen2']);p.add_argument('--declaration',required=True,type=Path);p.add_argument('--evidence',required=True,type=Path);a=p.parse_args()
    assert re.fullmatch('[a-z0-9][a-z0-9-]*',a.attempt)
    assert shutil.disk_usage(ROOT).free>=25_000_000_000
    evidence=a.evidence.resolve();evidence.mkdir(parents=True,exist_ok=True);run=ROOT/'build/run/t10-colony-scale'/a.attempt
    assert not (evidence/(a.phase+'-runner.json')).exists(),'Preserve each phase; choose a fresh attempt'
    if a.phase in ('prepare','reopen1','reopen2'):
        assert (run/'world/level.dat').is_file() and (evidence/'cohort.json').is_file(),'Same saved world and cohort required'
    else:
        run.mkdir(parents=True,exist_ok=False)
        (run/'eula.txt').write_text('eula=true\n',encoding='utf-8')
        (run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\npause-when-empty-seconds=0\nenable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\nmax-players=1\nlevel-seed=0\nlevel-type=minecraft:flat_all_dimensions\nspawn-monsters=false\nspawn-protection=0\nmax-tick-time=60000\n',encoding='utf-8')
    spec=importlib.util.spec_from_file_location('retention',ROOT/'scripts/retain-server-worlds.py');ret=importlib.util.module_from_spec(spec);spec.loader.exec_module(ret)
    leases=run.parent/'.leases';leases.mkdir(exist_ok=True);leasefile=leases/(a.attempt+'.lock');leasefile.touch(exist_ok=True);lease=ret.exclusive_handle(leasefile)
    args=['runColonyScaleServer','--console=plain','-PprimeAntsScaleAttempt='+a.attempt,'-PprimeAntsScalePhase='+a.phase,'-PprimeAntsScaleEvidence='+str(evidence),'-PprimeAntsScaleDeclaration='+str(a.declaration.resolve())]
    q=lambda x:"'"+str(x).replace("'","''")+"'"
    script=evidence/(a.phase+'-command.ps1');script.write_text('& '+q(ROOT/'scripts/Invoke-GradleEvidence.ps1')+' -Name '+q(a.phase+'-server')+' -GradleArgs @('+','.join(map(q,args))+') -EvidenceDirectory '+q(evidence)+'\nexit $LASTEXITCODE\n',encoding='utf-8')
    frozen=inputs();write(evidence/(a.phase+'-inputs-before.json'),frozen);world=run/'world'
    before={p.relative_to(world).as_posix():sha(p) for p in world.rglob('*') if p.is_file()};write(evidence/(a.phase+'-world-before.json'),before)
    start=time.time();code=None
    try:
        with (evidence/(a.phase+'-console.log')).open('wb') as log:
            child=subprocess.Popen(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(script)],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
            write(evidence/(a.phase+'-wrapper-pid.json'),dict(pid=child.pid,start_epoch=start))
            code=child.wait()
    finally:
        ret.close_handle(lease)
        unchanged=inputs()==frozen
        record=dict(attempt=a.attempt,phase=a.phase,args=args,wrapper_exit=code,start_epoch=start,end_epoch=time.time(),elapsed_seconds=time.time()-start,world=str(world),frozen_inputs_unchanged=unchanged,lease_released_after_exit=code is not None,free_bytes=shutil.disk_usage(ROOT).free)
        write(evidence/(a.phase+'-runner.json'),record)
        write(evidence/(a.phase+'-world-after.json'),{p.relative_to(world).as_posix():sha(p) for p in world.rglob('*') if p.is_file()})
        # ZIP entries preserve long parameterized HTML report paths without extending the Windows destination path.
        try:
            reports=[];archive=evidence/(a.phase+'-reports.zip')
            with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED) as z:
                for folder in ('build/test-results','build/reports'):
                    for file in (ROOT/folder).rglob('*'):
                        if file.is_file():
                            name=file.relative_to(ROOT).as_posix();z.write(file,name);reports.append(dict(path=name,sha256=sha(file)))
                z.writestr('manifest.json',json.dumps(reports,indent=2))
            record.update(report_archive=str(archive),report_archive_sha256=sha(archive));write(evidence/(a.phase+'-runner.json'),record)
        except Exception as ex:
            record['report_archive_error']=repr(ex);write(evidence/(a.phase+'-runner.json'),record);raise
    assert code==0 and unchanged,record
    assert (evidence/(a.phase+'-native-exit.json')).is_file() and json.loads((evidence/(a.phase+'-native-exit.json')).read_text())['exit_code']==0
    assert not (evidence/(a.phase+'-failure.json')).exists(),'Semantic server failure; preserve and diagnose'
    assert (evidence/(a.phase+'-stopped.json')).is_file() and (evidence/(a.phase+'-save.json')).is_file(),'Normal ordered save/stop required'
    print(json.dumps(record,indent=2))
if __name__=='__main__':main()
