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
from contextlib import contextmanager
import msvcrt

UUID_NAME = re.compile(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", re.I)
EXPECTED_ROOT = Path(__file__).resolve().parent.parent / "build/run/serverGameTest"


class ActiveRun(RuntimeError):
    pass


@contextmanager
def registry_lock(root):
    """Same first-byte lock as diagnostic Gradle registration; held through pruning."""
    root = safe_root(root)
    leases = root / ".leases"
    leases.mkdir(exist_ok=True)
    safe_root(leases)
    path = leases / "registry.lock"
    if path.exists() and (path.is_symlink() or path.resolve().parent != leases):
        raise RuntimeError("Linked registry lock")
    with path.open("a+b") as stream:
        if stream.seek(0, 2) == 0:
            stream.write(b"\0"); stream.flush()
        deadline = time.monotonic() + 60
        while True:
            stream.seek(0)
            try:
                msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
                break
            except OSError:
                if time.monotonic() >= deadline:
                    raise RuntimeError("Registration/pruning mutex did not become available")
                time.sleep(0.05)
        try:
            yield
        finally:
            stream.seek(0)
            msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)


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


def exclusive_handle(path, delete_guard=False):
    """Fail closed on a held session/Gradle lease or any unexplained open error."""
    if not path.exists():
        return None
    if path.is_symlink() or path.resolve() != path.absolute():
        raise RuntimeError(f"Linked lock: {path}")
    if os.name != "nt":
        raise RuntimeError("Production lock checks require Windows")
    api = ctypes.WinDLL("kernel32", use_last_error=True)
    api.CreateFileW.argtypes = [wintypes.LPCWSTR, wintypes.DWORD, wintypes.DWORD,
                               ctypes.c_void_p, wintypes.DWORD, wintypes.DWORD, wintypes.HANDLE]
    api.CreateFileW.restype = wintypes.HANDLE
    handle = api.CreateFileW(str(path), 0x80000000, 4 if delete_guard else 0, None, 3, 0, None)
    if handle == ctypes.c_void_p(-1).value:
        error = ctypes.get_last_error()
        if error in (32, 33):
            raise ActiveRun(f"Active/locked run: {path} (Windows {error})")
        raise RuntimeError(f"Unexplained lock error: {path} (Windows {error})")
    return handle


def close_handle(handle):
    if handle is not None:
        api = ctypes.WinDLL("kernel32", use_last_error=True)
        api.CloseHandle.argtypes = [wintypes.HANDLE]
        api.CloseHandle(handle)


def selection(root, current, keep=10, active=()):
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
    ordered = sorted(runs, key=lambda p: (p.stat().st_mtime_ns, p.name), reverse=True)
    protected = {current, *active}
    retained = [p for i, p in enumerate(ordered) if i < keep or p.name in protected]
    deleted = [p for p in ordered if p not in retained]
    return retained, deleted


def maintain(root, current):
    if Path(os.path.abspath(root)) != EXPECTED_ROOT:
        raise RuntimeError("Production deletion is restricted to this repository's server GameTest root")
    with registry_lock(root):
        return maintain_locked(Path(root), current)


def register(root, current):
    """Create and lease atomically against pruning. Caller holds the lease through JVM shutdown."""
    if Path(os.path.abspath(root)) != EXPECTED_ROOT or not UUID_NAME.fullmatch(current):
        raise RuntimeError("Registration restricted to this repository and a fresh UUID")
    root.mkdir(parents=True, exist_ok=True)
    with registry_lock(root):
        run = root / current
        if run.exists():
            raise RuntimeError("Fresh run UUID already exists")
        run.mkdir()
        path = root / ".leases" / (current + ".lock")
        path.write_text(str(os.getpid()), encoding="utf-8")
        lease = exclusive_handle(path)
        try:
            record = maintain_locked(root, current)
        except BaseException:
            close_handle(lease)
            raise
    return run, lease, record


def maintain_locked(root, current):
    # Check all old leases AND sessions, and hold available deletion guards until deletion ends.
    retained, candidates = selection(root, current)
    handles = []
    active = []
    try:
        for run in candidates:
            guards = []
            try:
                guards.append(exclusive_handle(root / ".leases" / (run.name + ".lock")))
                guards.append(exclusive_handle(run / "world/session.lock", True))
                guards.append(exclusive_handle(run / "session.lock", True))
            except ActiveRun:
                for handle in guards:
                    close_handle(handle)
                active.append(run.name)
            except BaseException:
                for handle in guards:
                    close_handle(handle)
                raise
            else:
                handles.extend(guards)
        newest = {p.name for p in sorted(retained+candidates, key=lambda p: (p.stat().st_mtime_ns, p.name), reverse=True)[:10]}
        retained, deleted = selection(root, current, active=active)
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
                  recorded_epoch=time.time(), active_older_exceptions=sorted(({current, *active})-newest),
                  serialized_registration_and_pruning=True)
    (Path(root) / current / "retention.json").write_text(json.dumps(record, indent=2), encoding="utf-8")
    print(f"Server world retention: current={current} kept={len(retained)} deleted={len(deleted)} "
          f"freeBefore={before} freeAfter={after}")
    return record


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--current", required=True)
    args = parser.parse_args()
    maintain(args.root, args.current)
