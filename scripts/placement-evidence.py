"""One scoped diagnostic invocation; archive every finished attempt, stop on nonzero exit."""
import argparse, hashlib, json, pathlib, re, subprocess, zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
def quote(value): return "'"+str(value).replace("'","''")+"'"
def main():
    p=argparse.ArgumentParser();p.add_argument('--evidence',required=True);p.add_argument('--name',required=True)
    group=p.add_mutually_exclusive_group(required=True);group.add_argument('--focus');group.add_argument('--seed',choices=['2026100501','42','8675309']);a=p.parse_args()
    evidence=pathlib.Path(a.evidence).resolve();evidence.mkdir(parents=True,exist_ok=True)
    if not re.fullmatch('[a-z0-9-]+',a.name):raise ValueError('Unsafe evidence name')
    if a.focus:
        args=['runGameTest','--console=plain','-PprimeAntsServerDiagnosticFilter=prime_ants_test:natural_placement_game_test_'+a.focus]
        report=ROOT/'build/test-results/gametest/server.xml'
    else:
        native=evidence/('seed-'+a.seed);native.mkdir(exist_ok=False)
        args=['runPlacementDiagnostic','--console=plain','-PprimeAntsPlacementSeed='+a.seed,
              '-PprimeAntsPlacementAttempt=t14-seed-'+a.seed,'-PprimeAntsPlacementEvidence='+str(native)]
        report=native/'server.xml'
    command=evidence/(a.name+'-command.ps1')
    command.write_text('& '+quote(ROOT/'scripts/Invoke-GradleEvidence.ps1')+' -Name '+quote(a.name)+' -GradleArgs @('+','.join(map(quote,args))+') -EvidenceDirectory '+quote(evidence)+'\n$code=$LASTEXITCODE\nexit $code\n',encoding='utf-8')
    with (evidence/(a.name+'-console.log')).open('wb') as console:
        result=subprocess.run(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(command)],cwd=ROOT,stdout=console,stderr=subprocess.STDOUT)
    metadata=json.loads((evidence/(a.name+'.json')).read_text(encoding='utf-8-sig'))
    log=(evidence/(a.name+'.log')).read_bytes();log=log.decode('utf-16' if log.startswith(b'\xff\xfe') else 'utf-8-sig')
    if report.exists(): (evidence/(a.name+'-server.xml')).write_bytes(report.read_bytes())
    if a.focus:
        match=re.search(r'Fresh server GameTest directory: (.+)',log)
        world=pathlib.Path(match.group(1).strip())/'world' if match else None
    else:world=ROOT/'build/run/t14-placement'/('t14-seed-'+a.seed)/'world'
    if world and world.exists():
        archive=evidence/(a.name+'-world.zip')
        if archive.exists():raise ValueError('Archive already exists')
        with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
            for f in world.rglob('*'):
                if f.is_file():z.write(f,f.relative_to(world).as_posix())
        metadata['world_archive']=str(archive);metadata['world_archive_sha256']=hashlib.sha256(archive.read_bytes()).hexdigest()
    metadata['checked_process_exit']=result.returncode
    (evidence/(a.name+'-archive.json')).write_text(json.dumps(metadata,indent=2),encoding='utf-8')
    print(json.dumps(metadata,indent=2))
    if result.returncode:print('\n'.join(log.splitlines()[-45:]))
    raise SystemExit(result.returncode)
if __name__=='__main__':main()
