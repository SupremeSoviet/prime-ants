"""Reject profile-specific absolute paths in tracked text; never print their contents."""
import argparse
from pathlib import Path
import re
import subprocess


PROFILE_PATH = re.compile(
    r"(?i)(?<![a-z0-9_])(?:[a-z]:/+users/|/mnt/[a-z]/+users/|/home[/])[^/\s<>%\"']+"
)


def text_content(data):
    if data.startswith((b'\xff\xfe', b'\xfe\xff')):
        return data.decode('utf-16')
    if b'\0' in data:
        return None
    # ASCII path characters remain detectable in legacy text encodings, too.
    return data.decode('utf-8-sig', errors='surrogateescape')


def offending_lines(data):
    content = text_content(data)
    if content is None:
        return []
    return [number for number, line in enumerate(content.splitlines(), 1)
            if PROFILE_PATH.search(re.sub(r'\\+', '/', line))]


def verify(root):
    # The index includes newly added text files as well as existing tracked files.
    names = subprocess.check_output(['git', 'ls-files', '--cached', '-z'], cwd=root)
    offenses = []
    checked = 0
    for name in sorted(set(names.decode('utf-8').split('\0')) - {''}):
        path = root / name
        if not path.is_file():
            continue  # A tracked deletion has no public text to publish.
        data = path.read_bytes()
        if text_content(data) is None:
            continue
        checked += 1
        offenses.extend((name, number) for number in offending_lines(data))
    for name, number in offenses:
        print(f'{name}:{number}: forbidden absolute profile path')
    if offenses:
        print(f'Public-path guard rejected {len(offenses)} line(s) in tracked text')
        return 1
    print(f'Verified public paths in {checked} tracked text files')
    return 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    raise SystemExit(verify(args.root.resolve()))
