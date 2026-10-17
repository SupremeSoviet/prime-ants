"""Opt-in fresh T19 normal-server chain. One lease protects the world through A/B/C.

Stops at the first failed gate or decode mismatch; never repairs/replaces a world.
Historical restart and scale runners remain unchanged.
"""
import argparse,hashlib,importlib.util,json,re,shutil,subprocess,time,zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def module(name,path):
    s=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
disk=module('persistence_disk',ROOT/'scripts/persistence-disk.py')
ret=module('retention',ROOT/'scripts/retain-server-worlds.py')
scale=module('scale',ROOT/'scripts/run-colony-scale.py')
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def archive(evidence,name):
    with zipfile.ZipFile(evidence/(name+'-reports.zip'),'x',zipfile.ZIP_DEFLATED) as z:
        files=[]
        for folder in ['build/test-results','build/reports']:
            for p in (ROOT/folder).rglob('*'):
                if p.is_file():n=p.relative_to(ROOT).as_posix();z.write(p,n);files.append(dict(path=n,sha256=disk.sha(p)))
        z.writestr('manifest.json',json.dumps(files,indent=2))

def preclose_deltas(live,actual):
    """Retain full differences between an observer and the later normal final save.

    This is NOT the actual-save/first-decode equality comparator. Native ordinary
    QueenFounding.save recomputes its readiness display while chunks unload.
    Only that independently witnessed preclose -> final-save display transition
    is explained here. Actual saved fields remain the exact decode comparator.
    """
    rows=[]
    def walk(a,b,path):
        if isinstance(a,dict) and isinstance(b,dict):
            for k in sorted(set(a)|set(b)):walk(a.get(k),b.get(k),path+[k])
        elif isinstance(a,list) and isinstance(b,list) and len(a)==len(b):
            for i,(x,y) in enumerate(zip(a,b)):walk(x,y,path+[i])
        elif a!=b:rows.append(dict(path=path,preclose=a,actual_disk=b))
    for group in ['actors','blocks','tables','tableRoots','terrain']:walk(live[group],actual[group],[group])
    for row in rows:
        path=row['path'];row['explained_native_readiness_display']=bool(len(path)==4 and path[0]=='actors' and path[2:]==['Founding','Reason'] and live['actors'].get(path[1],{}).get('AntForm')=='queen' and row['preclose']=='operational_open' and row['actual_disk']=='settled_not_ready_enclosure_chunk_unavailable')
    return dict(exact=not rows,differences=rows,unexplained=[r for r in rows if not r['explained_native_readiness_display']],scope='Preclose observation vs normal final-save state; every value retained. Actual-save vs first normal decode has no field exceptions or tolerances.')
def main():
    p=argparse.ArgumentParser();p.add_argument('--attempt',required=True);p.add_argument('--declaration',required=True,type=Path);p.add_argument('--evidence',required=True,type=Path);a=p.parse_args()
    assert re.fullmatch('[a-z0-9][a-z0-9-]*',a.attempt)
    assert read(a.declaration)['t19Persistence'] is True
    assert read(a.declaration)['snapshotFormat']==2 and not read(a.declaration).get('decodeProbe',False),'Diagnostic probes cannot be acceptance campaigns'
    evidence=a.evidence.resolve();evidence.mkdir(parents=True,exist_ok=False)
    run=ROOT/'build/run/t10-colony-scale'/a.attempt;run.mkdir(exist_ok=False)
    (run/'eula.txt').write_text('eula=true\n',encoding='utf-8')
    (run/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=0\nonline-mode=false\npause-when-empty-seconds=0\nenable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\nmax-players=1\nlevel-seed=0\nlevel-type=minecraft:flat_all_dimensions\nspawn-monsters=false\nspawn-protection=0\nmax-tick-time=60000\n',encoding='utf-8')
    leases=run.parent/'.leases';leases.mkdir(exist_ok=True);leasefile=leases/(a.attempt+'.lock');leasefile.touch(exist_ok=False);lease=ret.exclusive_handle(leasefile)
    frozen=scale.inputs();decl_hash=disk.sha(a.declaration);write(evidence/'frozen-inputs.json',frozen)
    record=dict(attempt=a.attempt,world=str((run/'world').resolve()),declaration_sha256=decl_hash,phases=[],scope='Normal flush/close only; first unaccepted boundary stops the chain',lease=str(leasefile),lease_released=False)
    world=run/'world';prior=None;identities=[];start=time.time()
    try:
        for phase in ['prepare','reopen1','reopen2']:
            assert shutil.disk_usage(ROOT).free>=25_000_000_000
            assert scale.inputs()==frozen and disk.sha(a.declaration)==decl_hash
            before=disk.tree(world);write(evidence/(phase+'-world-before.json'),before)
            if prior is not None:
                assert before==read(evidence/(prior+'-world-after.json')),'Saved world changed between processes'
                write(evidence/(phase+'-disk.json'),read(evidence/(prior+'-actual-disk.json')))
            archive(evidence,phase+'-before-gradle')
            args=['runColonyScaleServer','--console=plain','-PprimeAntsScaleAttempt='+a.attempt,'-PprimeAntsScalePhase='+phase,'-PprimeAntsScaleEvidence='+str(evidence),'-PprimeAntsScaleDeclaration='+str(a.declaration.resolve())]
            if phase=='prepare':args.append('-PprimeAntsScaleFresh=true')
            q=lambda v:"'"+str(v).replace("'","''")+"'"
            script=evidence/(phase+'-command.ps1');script.write_text("$ErrorActionPreference='Stop'\n$OutputEncoding=[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new($false)\n& "+q(ROOT/'scripts/Invoke-GradleEvidence.ps1')+' -Name '+q(phase+'-server')+' -GradleArgs @('+','.join(q(v) for v in args)+') -EvidenceDirectory '+q(evidence)+'\nexit $LASTEXITCODE\n',encoding='utf-8')
            t=time.time();item=dict(phase=phase,args=args,start_epoch=t,inputs_equal_before=True,world_equal_before=True)
            record['phases'].append(item);write(evidence/'chain.json',record)
            with (evidence/(phase+'-console.log')).open('xb') as log:
                child=subprocess.Popen(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(script)],cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
                item['wrapper_pid']=child.pid;write(evidence/'chain.json',record);code=child.wait()
            item.update(wrapper_exit=code,elapsed_seconds=time.time()-t,ended_epoch=time.time(),inputs_equal_after=scale.inputs()==frozen)
            write(evidence/(phase+'-world-after.json'),disk.tree(world));write(evidence/'chain.json',record)
            archive(evidence,phase+'-after-exit')
            if (evidence/(phase+'-process.json')).exists():
                native=read(evidence/(phase+'-process.json'));item['native']=native
                identity=(native['pid'],native['processStart']);assert identity not in identities;identities.append(identity)
                assert Path(native['world']).resolve()==world.resolve()
            if (evidence/(phase+'-native-exit.json')).exists():item['native_exit']=read(evidence/(phase+'-native-exit.json'))['exit_code']
            item['gradle_exit']=read(evidence/(phase+'-server.json')).get('exit_code');write(evidence/'chain.json',record)
            assert code==0 and item.get('native_exit')==0 and item['gradle_exit']==0 and item['inputs_equal_after'],item
            assert not (evidence/(phase+'-failure.json')).exists(),'Unaccepted semantic boundary: preserve world and diagnose'
            assert (evidence/(phase+'-stopped.json')).exists() and (evidence/(phase+'-save.json')).exists() and (evidence/(phase+'-qualified-boundary.json')).exists()
            live=read(evidence/(phase+'-before-close.json'))
            actual=disk.capture(world,live['terrain'],read(evidence/(phase+'-native-format.json')));write(evidence/(phase+'-actual-disk.json'),actual)
            differences=preclose_deltas(live,actual);write(evidence/(phase+'-live-to-actual-disk.json'),differences)
            assert not differences['unexplained'],'Unexplained qualified preclose to final-save delta; preserve first boundary'
            prior=phase
        record['accepted']=True
    finally:
        record.update(elapsed_seconds=time.time()-start,lease_released=True,free_bytes=shutil.disk_usage(ROOT).free)
        ret.close_handle(lease);write(evidence/'chain.json',record)
    print(json.dumps(record,indent=2))
if __name__=='__main__':main()
