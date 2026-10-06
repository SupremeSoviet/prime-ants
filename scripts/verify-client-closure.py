"""Ordinary 26.3 client closure check; no client launch or biological operations.

Call check_process_and_log after Popen.wait(), settle_saves before archiving, then
check_archive. A requested stop or a single save phrase never completes closure.
Historical records are read only; derived results belong in a new evidence file.
"""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import re
import time
import zipfile

DIMENSIONS = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"}
LINE = re.compile(r"^\[\d\d:\d\d:\d\d\] \[([^\]]+)/(INFO|WARN|ERROR|FATAL)\]: (.*)$")
SAVE = re.compile(r"^Saving chunks for level 'ServerLevel\[(.*)\]'/([\w.:-]+)$")
FATAL = re.compile(
    r"A fatal error has been detected|Exception in server tick loop|"
    r"(?:Exception|Error) (?:while |when )?(?:stopping|saving|closing)|"
    r"Failed to (?:save|stop)|Couldn't save|Uncaught exception|"
    r"Crash report saved|This crash report has been saved|Unreported exception thrown|"
    r"Exception in thread|Caught exception in thread|Encountered an unexpected exception|/FATAL\]",
    re.IGNORECASE,
)


def sha(path):
    with Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def world_spec(value):
    save, separator, name = value.partition("=")
    if not separator or not save or not name:
        raise ValueError("World must be SAVE_DIRECTORY=LOG_WORLD_NAME")
    path = Path(save)
    if path.is_absolute() or len(path.parts) != 1 or save in {".", ".."} or "/" in save or "\\" in save:
        raise ValueError("Expected one relative save directory name")
    return {"save": save, "name": name}


def check_process_and_log(record, text, worlds, console=""):
    errors = []
    # Historical launchers recorded wait()'s return, PID, end and elapsed time.
    # New launchers additionally record process_exited and forced_exit explicitly.
    ended = isinstance(record.get("end"), str) and bool(record["end"])
    waited = (type(record.get("pid")) is int and record["pid"] > 0
              and isinstance(record.get("elapsed_seconds"), (int, float))
              and record["elapsed_seconds"] >= 0 and ended)
    if type(record.get("exit_code")) is not int or record["exit_code"] != 0:
        errors.append("Process did not exit zero")
    if not waited or record.get("process_exited") is False:
        errors.append("Completed process wait evidence missing")
    if (any(record.get(key) for key in ("forced_exit", "forced", "terminated", "killed", "timed_out"))
            or record.get("exit_kind") in {"forced", "killed", "timeout"}):
        errors.append("Forced/interrupted exit is not normal closure")
    if not worlds:
        errors.append("Expected world openings must be declared")
    if FATAL.search(text + "\n" + console):
        errors.append("Fatal/save/teardown failure signature present")
    events = []
    for number, line in enumerate(text.splitlines(), 1):
        match = LINE.fullmatch(line)
        if match:
            events.append((number, *match.groups()))
    # These 26.3 messages precede a rethrown storage-flush exception and the
    # client's emergency-save/crash path respectively. Trailing stop/save lines
    # cannot repair either failure. Match real formatted logger events, so chat
    # quoting these words and ordinary network/Realms warnings remain harmless.
    for number, thread, level, message in events:
        if level in {"WARN", "ERROR", "FATAL"}:
            if re.fullmatch(r"IO-Worker-\d+", thread) and message == "Failed to synchronize chunks":
                errors.append(f"Line {number}: storage flush failed ({thread}/{level})")
            if thread == "Render thread" and message == "Reported exception thrown!":
                errors.append(f"Line {number}: reported runtime exception ({thread}/{level})")
    starts = [i for i, (_, thread, level, message) in enumerate(events)
              if thread == "Server thread" and level == "INFO"
              and message == "Starting integrated minecraft server version 26.3"]
    if len(starts) != len(worlds):
        errors.append(f"Expected {len(worlds)} openings; observed {len(starts)}")
    checks = []
    last_save_line = 0
    for opening, start in enumerate(starts):
        end = starts[opening + 1] if opening + 1 < len(starts) else len(events)
        segment = events[start:end]
        expected = worlds[opening] if opening < len(worlds) else None
        cursor = 0
        sequence = {}
        for phrase in ("Stopping server", "Saving players", "Saving worlds"):
            found = next((i for i in range(cursor, len(segment))
                          if segment[i][1:] == ("Server thread", "INFO", phrase)), None)
            if found is None:
                errors.append(f"Opening {opening + 1}: ordered {phrase} missing")
                break
            sequence[phrase] = segment[found][0]
            cursor = found + 1
        dimensions = {}
        if len(sequence) == 3:
            for line, thread, level, message in segment[cursor:]:
                match = SAVE.fullmatch(message)
                if thread == "Server thread" and level == "INFO" and match:
                    name, dimension = match.groups()
                    if expected is None or name != expected["name"]:
                        errors.append(f"Opening {opening + 1}: unexpected saved world {name}")
                    dimensions[dimension] = line
            if set(dimensions) != DIMENSIONS:
                errors.append(f"Opening {opening + 1}: terminal dimension saves incomplete/unexpected")
            if dimensions:
                last_save_line = max(last_save_line, *dimensions.values())
            if any(level in {"ERROR", "FATAL"} and thread == "Server thread"
                   for line, thread, level, message in segment):
                # Do not ignore an ERROR in this world's lifecycle. Realms/profile
                # network errors occur on other threads and are not teardown proof.
                errors.append(f"Opening {opening + 1}: server error present")
        checks.append({"opening": opening + 1, "start_line": segment[0][0],
                       "expected_world": expected, "stop_save_lines": sequence,
                       "dimension_save_lines": dimensions})
    stops = [line for line, thread, level, message in events
             if thread == "Render thread" and level == "INFO" and message == "Stopping!"]
    if len(stops) != 1 or not last_save_line or stops[0] <= last_save_line:
        errors.append("Client stopping must follow all world saves exactly once")
    return {"process_log_ok": not errors, "errors": errors, "openings": checks,
            "client_stop_lines": stops, "raw_normal_close": record.get("normal_close"),
            "process_wait_evidence": "explicit" if record.get("process_exited") is True else "historical wait record"}


def saved_snapshot(game_dir, worlds):
    game = Path(game_dir).resolve()
    result = {}
    for world in worlds:
        root = (game / "saves" / world["save"]).resolve()
        if not root.is_relative_to(game / "saves") or not (root / "level.dat").is_file():
            raise ValueError(f"Expected saved level.dat missing/unsafe: {world['save']}")
        for path in sorted(root.rglob("*")):
            if path.is_file():
                if path.is_symlink() or not path.resolve().is_relative_to(root):
                    raise ValueError("Save contains an external link")
                result[path.relative_to(game).as_posix()] = sha(path)
    return result


def settle_saves(game_dir, worlds, delay=0.5):
    first = saved_snapshot(game_dir, worlds)
    time.sleep(delay)
    second = saved_snapshot(game_dir, worlds)
    if first != second:
        raise ValueError("Saved files have not settled")
    return second


def check_archive(archive, snapshot):
    if not snapshot:
        raise ValueError("No settled saved files")
    with zipfile.ZipFile(archive) as z:
        names = z.namelist()
        if len(names) != len(set(names)) or z.testzip() is not None:
            raise ValueError("Archive has duplicate entries or invalid CRC")
        for name, expected in snapshot.items():
            if name not in names or hashlib.sha256(z.read(name)).hexdigest() != expected:
                raise ValueError(f"Archive missing/mismatched saved file: {name}")
        if any(Path(name).name.startswith("hs_err_pid") or "crash-reports/" in name for name in names):
            raise ValueError("Archive contains crash evidence")
    return {"archive": str(Path(archive).resolve()), "sha256": sha(archive),
            "crc_ok": True, "matching_saved_files": len(snapshot)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for option in ("record", "log", "console", "game-dir", "archive", "output"):
        parser.add_argument("--" + option, required=True)
    parser.add_argument("--world", action="append", required=True, type=world_spec)
    args = parser.parse_args()
    output = Path(args.output)
    if output.exists():
        raise SystemExit("Refusing to replace derived evidence")
    result = check_process_and_log(json.loads(Path(args.record).read_text(encoding="utf-8")),
                                   Path(args.log).read_text(encoding="utf-8", errors="strict"),
                                   args.world, Path(args.console).read_text(encoding="utf-8", errors="strict"))
    result["normal_close"] = False
    if result["process_log_ok"]:
        try:
            if Path(args.log).resolve() != (Path(args.game_dir) / "logs/latest.log").resolve():
                raise ValueError("Use the complete latest.log from the declared game directory")
            snapshot = settle_saves(args.game_dir, args.world)
            snapshot["logs/latest.log"] = sha(args.log)
            result["archive_check"] = check_archive(args.archive, snapshot)
            result["settled_saved_files"] = snapshot
            result["normal_close"] = True
        except (OSError, ValueError, zipfile.BadZipFile) as error:
            result["errors"].append(str(error))
    result["verified_utc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    output.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(json.dumps({"normal_close": result["normal_close"], "errors": result["errors"], "output": str(output)}))
    raise SystemExit(0 if result["normal_close"] else 1)


if __name__ == "__main__":
    main()
