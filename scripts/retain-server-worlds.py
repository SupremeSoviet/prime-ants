"""Keep ten fresh server GameTest runs. No client or evidence cleanup authority.

Gradle holds a lease outside its current world until runGameTest finishes. Old
world session locks are checked too, including runs started before leases existed.
Selection and every path/lock check finish before the first deletion.
"""
import argparse
import ctypes
from ctypes import wintypes
import json
import os
from pathlib import Path
import re
import shutil
import stat
import time

UUID_NAME = re.compile(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", re.I)
EXPECTED_ROOT = Path(__file__).resolve().parent.parent / "build/run/serverGameTest"


def safe_root(root):
    root = Path(os.path.abspath(root))
    if root.resolve() != root:
        raise RuntimeError(f"Root resolves through a link: {root}")
    if not root.is_dir():
        raise RuntimeError(f"Missing run root: {root}")
    return root


def safe_tree(root, run):
    if run.parent != root or not UUID_NAME.fullmatch(run.name):
        raise RuntimeError(f"Not a direct UUID run: {run}")
    for directory, dirs, files in os.walk(run, followlinks=False):
        for path in [Path(directory), *(Path(directory) / n for n in dirs + files)]:
            info = path.lstat()
            if path.is_symlink() or getattr(info, "st_file_attributes", 0) & stat.FILE_ATTRIBUTE_REPARSE_POINT:
                raise RuntimeError(f"Run contains a link/reparse point: {path}")
            if not path.resolve().is_relative_to(root) or path.resolve() == root:
                raise RuntimeError(f"Path escapes run root: {path}")


def exclusive_handle(path):
    """Fail closed on a held session/Gradle lease or any unexplained open error."""
    if not path.exists():
        return None
    if os.name != "nt":
        raise RuntimeError("Production lock checks require Windows")
    api = ctypes.WinDLL("kernel32", use_last_error=True)
    api.CreateFileW.argtypes = [wintypes.LPCWSTR, wintypes.DWORD, wintypes.DWORD,
                               ctypes.c_void_p, wintypes.DWORD, wintypes.DWORD, wintypes.HANDLE]
    api.CreateFileW.restype = wintypes.HANDLE
    handle = api.CreateFileW(str(path), 0x80000000, 0, None, 3, 0, None)
    if handle == ctypes.c_void_p(-1).value:
        raise RuntimeError(f"Active/locked run; deletion stopped: {path} (Windows {ctypes.get_last_error()})")
    return handle


def close_handle(handle):
    if handle is not None:
        api = ctypes.WinDLL("kernel32", use_last_error=True)
        api.CloseHandle.argtypes = [wintypes.HANDLE]
        api.CloseHandle(handle)


def selection(root, current, keep=10):
    root = safe_root(root)
    if not UUID_NAME.fullmatch(current):
        raise RuntimeError("Current run must be a UUID")
    runs = []
    for child in root.iterdir():
        if UUID_NAME.fullmatch(child.name):
            safe_tree(root, child)
            if not child.is_dir():
                raise RuntimeError(f"UUID entry is not a directory: {child}")
            runs.append(child)
    current_path = root / current
    if current_path not in runs:
        raise RuntimeError("Current run directory must exist before selection")
    others = sorted((p for p in runs if p != current_path),
                    key=lambda p: (p.stat().st_mtime_ns, p.name), reverse=True)
    retained = [current_path, *others[:keep - 1]]
    deleted = others[keep - 1:]
    return retained, deleted


def maintain(root, current):
    if Path(os.path.abspath(root)) != EXPECTED_ROOT:
        raise RuntimeError("Production deletion is restricted to this repository's server GameTest root")
    retained, deleted = selection(root, current)
    # Check all proposed deletions first. Leases remain held throughout deletion.
    handles = []
    try:
        for run in deleted:
            handles.append(exclusive_handle(Path(root) / ".leases" / (run.name + ".lock")))
            session = exclusive_handle(run / "world/session.lock")
            close_handle(session)
            session = exclusive_handle(run / "session.lock")
            close_handle(session)
        before = shutil.disk_usage(root).free
        for run in deleted:
            safe_tree(Path(root), run)  # Revalidate immediately before recursive deletion.
            shutil.rmtree(run)
        after = shutil.disk_usage(root).free
    finally:
        for handle in handles:
            close_handle(handle)
    record = dict(current=current, retained=[p.name for p in retained], deleted=[p.name for p in deleted],
                  deleted_count=len(deleted), free_before=before, free_after=after,
                  recorded_epoch=time.time())
    (Path(root) / current / "retention.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
    print(f"Server world retention: current={current} kept={len(retained)} deleted={len(deleted)} "
          f"freeBefore={before} freeAfter={after}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--current", required=True)
    args = parser.parse_args()
    maintain(args.root, args.current)
