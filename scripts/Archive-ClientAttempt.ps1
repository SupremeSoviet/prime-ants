param([Parameter(Mandatory=$true)][string]$Name,[Parameter(Mandatory=$true)][string]$EvidenceDirectory)
$ErrorActionPreference='Stop'
$workspace=Split-Path -Parent $PSScriptRoot
$saves=Join-Path $workspace 'build\run\clientGameTest\saves'
# Must be called immediately after the client task exits, including failed attempts, before any Gradle task.
$env:PRIME_ANTS_ARCHIVE_NAME=$Name
$env:PRIME_ANTS_ARCHIVE_EVIDENCE=$EvidenceDirectory
$env:PRIME_ANTS_ARCHIVE_SAVES=$saves
@'
import os,pathlib,zipfile,json,hashlib,datetime
root=pathlib.Path(os.environ['PRIME_ANTS_ARCHIVE_SAVES']);out=pathlib.Path(os.environ['PRIME_ANTS_ARCHIVE_EVIDENCE']);name=os.environ['PRIME_ANTS_ARCHIVE_NAME']
out.mkdir(parents=True,exist_ok=True);target=out/(name+'-world.zip')
if target.exists():raise SystemExit('Refusing to replace prior world archive')
files=sorted(p for p in root.rglob('*') if p.is_file()) if root.exists() else []
with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as z:
 for p in files:z.write(p,p.relative_to(root))
record={'source':str(root),'archive':str(target),'archived_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'files':len(files),'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'level_dat': [str(p.relative_to(root)) for p in files if p.name=='level.dat'],'save_status':'saved-world-present' if any(p.name=='level.dat' for p in files) else 'incomplete-no-level-dat','file_hashes':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}}
(out/(name+'-world-archive.json')).write_text(json.dumps(record,indent=2),encoding='utf-8');print(json.dumps({k:v for k,v in record.items() if k!='file_hashes'},indent=2))
'@ | python -
if($LASTEXITCODE -ne 0){throw 'World archival failed; do not run another Gradle task'}
