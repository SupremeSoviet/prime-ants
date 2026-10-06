"""T27 dedicated natural-world observer. Existing historical acceptance/restart tools are unchanged."""
import argparse, hashlib, json, pathlib, subprocess, time, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write(path, value):
    path.write_text(json.dumps(value, indent=2), encoding='utf-8')


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--attempt', required=True)
    p.add_argument('--phase', required=True, choices=['t27-measure', 't27-reopen1', 't27-reopen2'])
    p.add_argument('--declaration', required=True, type=pathlib.Path)
    p.add_argument('--evidence', required=True, type=pathlib.Path)
    a = p.parse_args()
    assert a.attempt.replace('-', '').isalnum() and a.attempt == a.attempt.lower()
    evidence = a.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=True)
    declaration = json.loads(a.declaration.read_text(encoding='utf-8'))
    source = pathlib.Path(declaration['source'])
    assert sha(source) == declaration['sha256'], 'Declared natural source hash changed'
    run = ROOT / 'build/run/t09-restart' / a.attempt
    world = run / 'world'
    assert (world / 'level.dat').is_file(), 'Use an inspected task-owned copy of the declared source'
    name = a.phase + '-server'
    assert not (evidence / (name + '.json')).exists(), 'Preserve each attempt, do not overwrite'
    before = {str(f.relative_to(world)): sha(f) for f in world.rglob('*') if f.is_file()}
    write(evidence / (a.phase + '-files-before.json'), before)
    args = ['runRestartServer', '--console=plain', '-PprimeAntsRestartAttempt=' + a.attempt,
            '-PprimeAntsRestartPhase=' + a.phase, '-PprimeAntsRestartEvidence=' + str(evidence),
            '-PprimeAntsReleaseSource=' + str(a.declaration.resolve())]
    q = lambda s: "'" + str(s).replace("'", "''") + "'"
    ps = evidence / (a.phase + '-command.ps1')
    ps.write_text('& ' + q(ROOT / 'scripts/Invoke-GradleEvidence.ps1') + ' -Name ' + q(name) +
                  ' -GradleArgs @(' + ','.join(map(q, args)) + ') -EvidenceDirectory ' + q(evidence) +
                  '\nexit $LASTEXITCODE\n', encoding='utf-8')
    command = ['powershell.exe', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(ps)]
    frozen_paths = list((ROOT / "src").rglob("*")) + list((ROOT / "gradle").rglob("*")) + list((ROOT / "build/classes").rglob("*")) + list((ROOT / "build/resources").rglob("*")) + [ROOT / "build.gradle", ROOT / "gradle.properties", pathlib.Path(__file__)]
    frozen = {str(f.relative_to(ROOT)): sha(f) for f in frozen_paths if f.is_file()}
    write(evidence / (a.phase + "-inputs-before.json"), frozen)
    start = time.monotonic()
    with (evidence / (a.phase + '-console.log')).open('wb') as log:
        result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    record = {'phase': a.phase, 'argv': command, 'gradle_args': args, 'exit_code': result.returncode,
              'elapsed_seconds': time.monotonic() - start, 'world': str(world),
              'source_preserved': sha(source) == declaration['sha256']}
    archive = evidence / (a.phase + '-world.zip')
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
        for f in world.rglob('*'):
            if f.is_file():
                z.write(f, f.relative_to(world))
    record.update(archive=str(archive), archive_sha256=sha(archive),
                  frozen_inputs_unchanged=all((ROOT / f).exists() and sha(ROOT / f) == h for f, h in frozen.items()))
    write(evidence / (a.phase + '-execution.json'), record)
    assert record["frozen_inputs_unchanged"]
    assert result.returncode == 0 and not (evidence / (a.phase + '-failure.json')).exists(), 'Preserved failed run; diagnose before another launch'
    assert (evidence / (a.phase + '-before-close.json')).exists() and (evidence / (a.phase + '-stopped.json')).exists(), 'Full graceful checkpoint missing'
    if a.phase == 't27-measure':
        performance = json.loads((evidence / 't27-performance.json').read_text())
        rows = performance['samples']
        assert len(rows) == 2400 and performance['warmup_ticks'] == 600
        assert all(r['tickrate'] == 20 and not r['sprint'] and r['queens'] >= 2 and r['ticking_adults'] >= 20 and r['native_tick_work_ns'] > 0 for r in rows)
        assert all(y['tick'] == x['tick'] + 1 for x, y in zip(rows, rows[1:]))
        # This proves tick advancement, rather than counting loaded-but-idle actors.
        assert all(y['ages'][k] == age + 1 for x, y in zip(rows, rows[1:]) for k, age in x['ages'].items() if k in y['ages'])
    print(json.dumps(record, indent=2))


if __name__ == '__main__':
    main()
