"""Small disposable fixture; it never calls production deletion."""
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import uuid
import multiprocessing
import time

spec = importlib.util.spec_from_file_location("retention", Path(__file__).with_name("retain-server-worlds.py"))
retention = importlib.util.module_from_spec(spec)
spec.loader.exec_module(retention)


def concurrent_registration(folder, name, queue):
    retention.EXPECTED_ROOT = Path(folder)
    run, lease, record = retention.register(Path(folder), name)
    try:
        time.sleep(0.3)
        queue.put(record)
    finally:
        retention.close_handle(lease)


class RetentionFixture(unittest.TestCase):
    def test_newest_ten_includes_current_and_ignores_nonruns(self):
        with tempfile.TemporaryDirectory(prefix="prime-ants-retention-") as folder:
            root = Path(folder)
            names = [str(uuid.uuid4()) for _ in range(14)]
            for i, name in enumerate(names):
                (root / name).mkdir()
                os.utime(root / name, (1000 + i, 1000 + i))
            (root / "archive").mkdir()
            (root / "notes.txt").write_text("preserve", encoding="utf-8")
            kept, removed = retention.selection(root, names[0])
            self.assertEqual({p.name for p in kept}, {names[0], *names[-10:]})
            self.assertEqual({p.name for p in removed}, set(names[1:4]))
            self.assertTrue(all(p.exists() for p in removed))
            with self.assertRaisesRegex(RuntimeError, "restricted"):
                retention.maintain(root, names[0])
            with self.assertRaisesRegex(RuntimeError, "UUID"):
                retention.selection(root, "../escape")

    def test_real_junction_escape_is_rejected_before_selection(self):
        with tempfile.TemporaryDirectory(prefix="prime-ants-retention-") as folder:
            base = Path(folder)
            root = base / "runs"
            outside = base / "outside"
            root.mkdir()
            outside.mkdir()
            current = str(uuid.uuid4())
            (root / current).mkdir()
            link = root / str(uuid.uuid4())
            subprocess.run(["cmd.exe", "/c", "mklink", "/J", str(link), str(outside)], check=True, capture_output=True)
            try:
                with self.assertRaisesRegex(RuntimeError, "link|escapes"):
                    retention.selection(root, current)
                with self.assertRaisesRegex(RuntimeError, "link"):
                    retention.safe_root(link)
                self.assertTrue(outside.exists())
            finally:
                link.rmdir()  # Only the verified disposable junction itself.

    def test_locked_world_is_protected(self):
        with tempfile.TemporaryDirectory(prefix="prime-ants-retention-") as folder:
            lock = Path(folder) / "session.lock"
            lock.write_bytes(b"fixture")
            handle = retention.exclusive_handle(lock)
            try:
                with self.assertRaisesRegex(RuntimeError, "Active/locked"):
                    retention.exclusive_handle(lock)
            finally:
                retention.close_handle(handle)
            retention.close_handle(retention.exclusive_handle(lock))

    def test_two_real_registrars_protect_old_active_outside_newest_ten(self):
        with tempfile.TemporaryDirectory(prefix="prime-ants-retention-concurrent-") as folder:
            root = Path(folder); (root / ".leases").mkdir()
            names = [str(uuid.uuid4()) for _ in range(14)]
            for i, name in enumerate(names):
                (root / name).mkdir(); os.utime(root / name, (1000+i, 1000+i))
            lock = root / ".leases" / (names[0]+".lock"); lock.write_bytes(b"old active")
            lease = retention.exclusive_handle(lock); queue = multiprocessing.Queue()
            children = [multiprocessing.Process(target=concurrent_registration, args=(folder,str(uuid.uuid4()),queue)) for _ in range(2)]
            try:
                for p in children: p.start()
                records = [queue.get(timeout=30) for _ in children]
                for p in children: p.join(30); self.assertEqual(0,p.exitcode)
                self.assertTrue((root/names[0]).exists())
                self.assertEqual(11, len([p for p in root.iterdir() if retention.UUID_NAME.fullmatch(p.name)]))
                for r in records:
                    self.assertIn(names[0],r['retained']); self.assertIn(names[0],r['active_older_exceptions'])
                    self.assertTrue(r['serialized_registration_and_pruning'])
            finally:
                retention.close_handle(lease)
            original = retention.EXPECTED_ROOT
            try:
                retention.EXPECTED_ROOT = root
                result = retention.maintain(root, records[-1]['current'])
                self.assertNotIn(names[0],result['retained']); self.assertFalse((root/names[0]).exists())
                self.assertEqual(10,len(result['retained']))
            finally:
                retention.EXPECTED_ROOT = original

    def test_old_session_lock_is_retained_and_link_safeguards_still_apply(self):
        with tempfile.TemporaryDirectory(prefix="prime-ants-retention-session-") as folder:
            root=Path(folder); names=[str(uuid.uuid4()) for _ in range(12)]
            for i,name in enumerate(names):
                (root/name).mkdir();os.utime(root/name,(1000+i,1000+i))
            (root/names[0]/'world').mkdir(); path=root/names[0]/'world/session.lock';path.write_bytes(b'active')
            os.utime(root/names[0],(1000,1000));lease=retention.exclusive_handle(path);original=retention.EXPECTED_ROOT
            try:
                retention.EXPECTED_ROOT=root;result=retention.maintain(root,names[-1])
                self.assertIn(names[0],result['active_older_exceptions']);self.assertTrue(path.exists())
            finally:
                retention.EXPECTED_ROOT=original;retention.close_handle(lease)


if __name__ == "__main__":
    unittest.main(verbosity=2)
