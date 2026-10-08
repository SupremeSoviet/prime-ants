"""Small disposable fixture; it never calls production deletion."""
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import uuid

spec = importlib.util.spec_from_file_location("retention", Path(__file__).with_name("retain-server-worlds.py"))
retention = importlib.util.module_from_spec(spec)
spec.loader.exec_module(retention)


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
            self.assertEqual({p.name for p in kept}, {names[0], *names[-9:]})
            self.assertEqual({p.name for p in removed}, set(names[1:5]))
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


if __name__ == "__main__":
    unittest.main(verbosity=2)
