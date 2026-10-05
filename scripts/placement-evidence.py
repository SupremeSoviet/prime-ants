"""One scoped diagnostic invocation; archive every finished attempt, stop on nonzero exit."""
import argparse, hashlib, json, pathlib, re, subprocess, zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
def quote(value): return "'"+str(value).replace("'","''")+"'"
def main():
    p=argparse.ArgumentParser();p.add_argument('--evidence',required=True);p.add_argument('--name',required=True)
    group=p.add_mutually_exclusive_group(required=True);group.add_argument('--focus');group.add_argument('--case-id');group.add_argument('--seed',choices=['2026100501','42','8675309'])
    p.add_argument('--mode',choices=['t14','t15-biome-v1','t16-frozen-v1','t17-nectar-v1'],default='t14');p.add_argument('--declaration');p.add_argument('--attempt');p.add_argument('--prior-full-count',type=int,default=0);a=p.parse_args()
    evidence=pathlib.Path(a.evidence).resolve();evidence.mkdir(parents=True,exist_ok=True)
    if not re.fullmatch('[a-z0-9-]+',a.name):raise ValueError('Unsafe evidence name')
    if a.focus or a.case_id:
        case=a.case_id or 'prime_ants_test:natural_placement_game_test_'+a.focus
        if not re.fullmatch('prime_ants_test:[a-z0-9_]+',case):raise ValueError('Exact registered case ID required')
        args=['runGameTest','--console=plain','-PprimeAntsServerDiagnosticFilter='+case]
        report=ROOT/'build/test-results/gametest/server.xml'
    else:
        if a.mode in ['t15-biome-v1','t16-frozen-v1','t17-nectar-v1'] and (not a.declaration or not pathlib.Path(a.declaration).is_file()):raise ValueError('Frozen declaration required')
        turn='t17' if a.mode=='t17-nectar-v1' else 't16' if a.mode=='t16-frozen-v1' else 't15' if a.mode=='t15-biome-v1' else 't14'
        if a.mode in ['t16-frozen-v1','t17-nectar-v1']:
            # Count every closed T16 attempt, including failures/recoveries, before allocating a fresh world.
            ledgers=list(evidence.rglob('stopped.json'))
            counts={2026100501:0,42:0,8675309:0}
            for previous in ledgers:
                row=json.loads(previous.read_text());counts[row['seed']]+=row['total_full_chunks']
            if counts[int(a.seed)]!=a.prior_full_count:raise ValueError('Recovery prior FULL count does not match closed attempts')
            if counts[int(a.seed)]>=150 or sum(counts.values())>=(150 if a.mode=='t17-nectar-v1' else 450):raise ValueError('Cumulative T16 FULL budget exhausted')
            (evidence/(a.name+'-budget-before.json')).write_text(json.dumps({'by_seed':counts,'total':sum(counts.values()),'new_biome_queries':0},indent=2))
        attempt=a.attempt or turn+'-seed-'+a.seed
        if not re.fullmatch(turn+'-seed-[a-z0-9-]+',attempt):raise ValueError('Unsafe owned attempt')
        world=ROOT/('build/run/'+turn+'-placement')/attempt/'world'
        if world.parent.exists():raise ValueError('Existing native attempt directory refused')
        native=evidence/('seed-'+a.seed);native.mkdir(exist_ok=False)
        args=['runPlacementDiagnostic','--console=plain','-PprimeAntsPlacementSeed='+a.seed,
              '-PprimeAntsPlacementAttempt='+attempt,'-PprimeAntsPlacementEvidence='+str(native),'-PprimeAntsPlacementMode='+a.mode]
        if a.declaration:args+=['-PprimeAntsPlacementDeclaration='+str(pathlib.Path(a.declaration).resolve())]
        if a.prior_full_count:args+=['-PprimeAntsPlacementPriorFull='+str(a.prior_full_count)]
        report=native/'server.xml'
    command=evidence/(a.name+'-command.ps1')
    command.write_text('& '+quote(ROOT/'scripts/Invoke-GradleEvidence.ps1')+' -Name '+quote(a.name)+' -GradleArgs @('+','.join(map(quote,args))+') -EvidenceDirectory '+quote(evidence)+'\n$code=$LASTEXITCODE\nexit $code\n',encoding='utf-8')
    with (evidence/(a.name+'-console.log')).open('wb') as console:
        result=subprocess.run(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',str(command)],cwd=ROOT,stdout=console,stderr=subprocess.STDOUT)
    metadata=json.loads((evidence/(a.name+'.json')).read_text(encoding='utf-8-sig'))
    log=(evidence/(a.name+'.log')).read_bytes();log=log.decode('utf-16' if log.startswith(b'\xff\xfe') else 'utf-8-sig')
    if report.exists(): (evidence/(a.name+'-server.xml')).write_bytes(report.read_bytes())
    if a.focus or a.case_id:
        match=re.search(r'Fresh server GameTest directory: (.+)',log)
        world=pathlib.Path(match.group(1).strip())/'world' if match else None
    else:world=ROOT/('build/run/'+turn+'-placement')/attempt/'world'
    if world and world.exists():
        archive=evidence/(a.name+'-world.zip')
        if archive.exists():raise ValueError('Archive already exists')
        with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
            for f in world.rglob('*'):
                if f.is_file():z.write(f,f.relative_to(world).as_posix())
        metadata['world_archive']=str(archive);metadata['world_archive_sha256']=hashlib.sha256(archive.read_bytes()).hexdigest()
    if a.seed and a.mode in ['t16-frozen-v1','t17-nectar-v1']:
        closed=native/'stopped.json'
        if not closed.exists():raise ValueError('Closed-attempt FULL accounting missing; stop and audit archive before recovery')
        row=json.loads(closed.read_text());metadata['generated_full_chunks']=row['total_full_chunks']
        counts={2026100501:0,42:0,8675309:0}
        for previous in evidence.rglob('stopped.json'):
            old=json.loads(previous.read_text());counts[old['seed']]+=old['total_full_chunks']
        metadata['cumulative_full_by_seed']=counts;metadata['cumulative_full_total']=sum(counts.values())
        if any(v>150 for v in counts.values()) or sum(counts.values())>(150 if a.mode=='t17-nectar-v1' else 450):raise ValueError('Cumulative T16 FULL budget breached')
    metadata['checked_process_exit']=result.returncode
    (evidence/(a.name+'-archive.json')).write_text(json.dumps(metadata,indent=2),encoding='utf-8')
    print(json.dumps(metadata,indent=2))
    if result.returncode:print('\n'.join(log.splitlines()[-45:]))
    raise SystemExit(result.returncode)
if __name__=='__main__':main()
