"""Small disposable Git index fixture for the public-path build guard."""
import json
from pathlib import Path
import subprocess
import tempfile


GUARD = Path(__file__).with_name('verify-public-paths.py')


def main():
    with tempfile.TemporaryDirectory(prefix='prime-ants-public-paths-') as directory:
        root = Path(directory)
        subprocess.run(['git', 'init', '--quiet', str(root)], check=True)
        # Assemble sensitive-looking fixtures; no literal profile path is committed.
        windows = 'C:' + chr(92) + chr(92).join(['Users', 'private-owner', 'Documents', 'fixture'])
        fixtures = [windows, windows.replace(chr(92), '/'), json.dumps(windows),
                    '/mnt/c/' + '/'.join(['Users', 'private-owner', 'Documents', 'fixture']),
                    '/home/' + 'private-owner/fixture']
        (root / 'new-text.txt').write_text('\n'.join(fixtures), encoding='utf-8')
        (root / 'wide-text.txt').write_text(windows, encoding='utf-16')
        (root / 'asset.bin').write_bytes(b'\0' + windows.encode())
        (root / 'guard-source.py').write_bytes(GUARD.read_bytes())
        subprocess.run(['git', '-c', 'core.autocrlf=false', 'add', '--all'], cwd=root, check=True)
        command = ['python', str(GUARD), '--root', str(root)]
        rejected = subprocess.run(command, capture_output=True, text=True, check=False)
        assert rejected.returncode == 1, rejected.stdout
        assert all(f'new-text.txt:{n}:' in rejected.stdout for n in range(1, 6))
        assert 'wide-text.txt:1:' in rejected.stdout and 'asset.bin:' not in rejected.stdout
        assert 'private-owner' not in rejected.stdout + rejected.stderr
        (root / 'new-text.txt').write_text('docs/screenshots/frame.png\n<turnloop>/directions/T09\n'
                                         '<original-project>/src\n%USERPROFILE%/Documents/prime-ants\n'
                                         'C:/Users/%USERNAME%/Documents/example\nlineage/home/stage\ncache/home/care\n', encoding='utf-8')
        (root / 'wide-text.txt').write_text('docs/relative.md', encoding='utf-16')
        allowed = subprocess.run(command, capture_output=True, text=True, check=False)
        assert allowed.returncode == 0, allowed.stdout
        print('Public-path fixtures: 6 forbidden lines rejected; placeholders/relative paths allowed; binary preserved; contents hidden')


if __name__ == '__main__':
    main()
