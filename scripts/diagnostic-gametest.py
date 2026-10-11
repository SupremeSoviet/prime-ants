"""Freeze registered diagnostic identities before launch; retain red gameplay results.

No aliases, result-derived expectations, or XML rewriting. Compilation must precede freeze.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import shutil
import time
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('harness', Path(__file__).with_name('gametest-harness.py'))
h = importlib.util.module_from_spec(spec); spec.loader.exec_module(h)

def methods(source):
    rows = {}
    common = source.split('    private void run(GameTestHelper c,Mode mode){', 1)
    common_hash = hashlib.sha256(common[1].encode()).hexdigest() if len(common) == 2 else None
    for match in re.finditer(r'@GameTest(?:\(([^)]*)\))?\s+public void (\w+)\(GameTestHelper \w+\)\s*\{', source):
        at = match.end(); depth = 1; end = at
        while depth and end < len(source):
            depth += (source[end] == '{') - (source[end] == '}'); end += 1
        if depth: raise ValueError('Unclosed registered method')
        body = source[at:end-1]; mode = re.fullmatch(r'run\(c,Mode\.(\w+)\);', body.strip())
        rows[match[2]] = dict(annotation=match[1] or '', body_sha256=hashlib.sha256(body.encode()).hexdigest(),
                             mode=mode[1] if mode else None, common_body_sha256=common_hash)
    return rows

def correspondence(names):
    declared = h.discover(); result = {}
    for name in names:
        row = declared[name]; source = (h.ROOT/row['source']).read_text(encoding='utf-8')
        cls = Path(row['source']).stem; registered = methods(source)
        choices = [m for m in registered if 'prime_ants_test:'+h.snake(cls)+'_'+h.snake(m) == name]
        if len(choices) != 1: raise ValueError('Registered method correspondence: '+name)
        result[name] = dict(row, method=choices[0], **registered[choices[0]])
    return result

def copy_equivalence(mapping):
    """A selected copy must have its original plus both identical wrappers, at the same bounds."""
    groups={}
    for name,row in mapping.items():
        copied=re.fullmatch(r'copy([12])([A-Z]\w*)',row['method'])
        method=copied[2][0].lower()+copied[2][1:] if copied else row['method']
        groups.setdefault((row['source'],method),[]).append((name,row,copied[1] if copied else 'original'))
    evidence=[]
    for (source,method),rows in sorted(groups.items()):
        if not any(label!='original' for _,_,label in rows): continue
        if sorted(label for _,_,label in rows)!=['1','2','original']:raise ValueError('Copy assignment lacks original plus two: '+method)
        keys=('max_ticks','annotation','mode','body_sha256','common_body_sha256')
        if len({tuple(row[k] for k in keys) for _,row,_ in rows})!=1:raise ValueError('Copy body/mode/bounds differ: '+method)
        evidence.append(dict(source=source,method=method,names=sorted(name for name,_,_ in rows),**{k:rows[0][1][k] for k in keys}))
    return evidence

def native_exit_code(text):
    # PowerShell's native stderr formatting can wrap the Gradle exception at a space.
    exits=re.findall(r"Process 'command '[^']*java.exe'' finished with non-zero exit\s+value\s+(\d+)",text)
    return int(exits[-1]) if exits else (0 if 'GAME TESTS COMPLETE' in text and 'Game test server shutting down' in text else None)

def freeze(folder, launch, names):
    folder = Path(folder); folder.mkdir(parents=True, exist_ok=False)
    if not names or len(names) != len(set(names)): raise ValueError('Empty/duplicate assignment')
    mapping = correspondence(names)
    data = dict(label='pre-run exact diagnostic assignment', captured_epoch=time.time(), expected_names=names,
                correspondence=mapping, copy_equivalence=copy_equivalence(mapping), launch=launch, inputs=h.inputs(launch))
    # Archive the actual executable sources and compiled classes for this diagnostic compilation.
    for source in {v['source'] for v in mapping.values()}:
        path = h.ROOT/source; shutil.copyfile(path, folder/path.name)
        for compiled in (h.ROOT/'build/classes/java/gametest/dev/primeants/gametest').glob(path.stem+'*.class'):
            shutil.copyfile(compiled,folder/compiled.name)
    h.write(folder/'frozen.json', data); return data

def collect(folder, report, start, end, exit_code, log=None):
    folder = Path(folder); frozen = h.read(folder/'frozen.json')
    cases, issues = h.validate_report(report, frozen['expected_names'], start, end, exit_code)
    if frozen['captured_epoch'] > start: issues.append('assignment was not frozen before run')
    if h.inputs(frozen['launch']) != frozen['inputs']: issues.append('executable inputs changed during execution')
    if correspondence(frozen['expected_names']) != frozen['correspondence']: issues.append('registered method/mode/body/bounds changed')
    if 'copy_equivalence' in frozen and copy_equivalence(frozen['correspondence'])!=frozen['copy_equivalence']:issues.append('copy equivalence differs from freeze')
    failures = [dict(name=c.get('name'), message=f.get('message'), text=f.text) for c in cases for f in c.iter('failure')]
    errors = [dict(name=c.get('name'), message=f.get('message')) for c in cases for f in c.iter('error')]
    skips = [dict(name=c.get('name'), message=f.get('message')) for c in cases for f in c.iter('skipped')]
    try:
        root = h.supported_report_root(report)
        for suite in root.iter('testsuite'):
            for attr, tag in [('failures','failure'),('errors','error'),('skipped','skipped')]:
                if attr in suite.attrib and int(suite.get(attr)) != sum(any(True for _ in c.iter(tag)) for c in suite.iter('testcase')):
                    issues.append('inaccurate declared '+attr+' total')
    except (OSError, ET.ParseError, ValueError) as error: issues.append(str(error))
    infrastructure = [i for i in issues if i not in ('nonzero failures','report failure','failed child exit: 1')]
    if exit_code != 0 and not (exit_code == 1 and failures): infrastructure.append('unexplained native/launcher exit')
    if errors or skips: infrastructure.append('native error/skip')
    protection = {};native_exit=None
    text = None
    if log is None:
        infrastructure.append('missing log evidence: --log omitted')
    else:
        try:
            text = Path(log).read_text(encoding='utf-8-sig')
        except (OSError, UnicodeError) as error:
            infrastructure.append('missing log evidence: unreadable '+str(log)+': '+str(error))
    if text is not None:
        for task in ('deleteGameTestRunDir','acceptGameTestEula'):
            protection[task] = bool(re.search(r'> Task :'+task+r' SKIPPED', text))
            if not protection[task]: infrastructure.append('client guard not SKIPPED: '+task)
        if 'GAME TESTS COMPLETE' not in text or 'Game test server shutting down' not in text:
            infrastructure.append('native completion/shutdown missing')
        native_exit=native_exit_code(text)
        if native_exit!=(1 if failures else 0):infrastructure.append('native exit disagrees with report')
    archived = folder/'server.xml'
    if Path(report).resolve()!=archived.resolve(): shutil.copyfile(report, archived)
    # Gameplay describes the extracted XML rows. Exit, identity, freshness, input
    # and log/guard rejection belong to infrastructure and cannot make CLI success.
    gameplay_issues = [i for i in issues if i in ('nonzero failures','report failure',
        'nonzero errors','report error','nonzero skipped','report skipped')]
    result = dict(expected=frozen['expected_names'], executed=[c.get('name') for c in cases],
                  count=len(cases), start_epoch=start,end_epoch=end,seconds=end-start,exit_code=exit_code,
                  native_exit_code=native_exit,
                  original_mtime=Path(report).stat().st_mtime, xml_sha256=h.sha(archived),
                  issues=issues,infrastructure_issues=infrastructure,infrastructure_valid=not infrastructure,
                  gameplay_issues=gameplay_issues,gameplay_passed=bool(cases) and not gameplay_issues,
                  failures=failures,errors=errors,skips=skips,protection=protection)
    h.write(folder/'validation.json',result);return result

def main():
    p=argparse.ArgumentParser();p.add_argument('action',choices=['freeze','collect']);p.add_argument('--folder',type=Path,required=True)
    p.add_argument('--launch',type=Path);p.add_argument('--names');p.add_argument('--report',type=Path);p.add_argument('--invocation',type=Path);p.add_argument('--log',type=Path)
    a=p.parse_args()
    if a.action=='freeze':
        result=freeze(a.folder,h.read(a.launch),a.names.split(',')); print('FROZEN',len(result['expected_names']))
    else:
        inv=h.read(a.invocation);result=collect(a.folder,a.report,inv['start_epoch'],inv['end_epoch'],inv['exit_code'],a.log)
        print(json.dumps({k:result[k] for k in ['count','exit_code','infrastructure_valid','gameplay_passed','infrastructure_issues','failures']},indent=2))
        raise SystemExit(0 if result['infrastructure_valid'] and result['gameplay_passed'] else 1)
if __name__=='__main__':main()
