# fim-java

[![CI](https://github.com/Afaguayo/fim-java/actions/workflows/ci.yml/badge.svg)](https://github.com/Afaguayo/fim-java/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A small, dependency-light **file integrity monitor** written in Java. Take a
SHA-256 baseline of a directory tree, come back later (or run on a timer),
and find out exactly what was added, removed, or modified — and what the
hash changed from/to.

## Why

Tripwire and AIDE are the classic tools for this, but they're heavy to set
up for a side project, a CI artifact, or a small VPS. `fim-java` is a single
jar: baseline now, check later, done.

## How it works

1. `baseline` walks a directory recursively, computes a streaming SHA-256
   for every regular file, and writes `{relative/path: hash}` to a JSON
   file.
2. `check` recomputes the same walk and diffs it against the stored
   baseline: files present now but not before are **added**, files in the
   baseline but missing now are **removed**, and files whose hash changed
   are **modified** (reported with both the old and new hash).
3. `watch` repeats `check` on an interval and prints a timestamped alert
   whenever a diff is non-empty — a crude but effective polling watcher
   with no native OS file-event dependency.

## Build

Requires JDK 17+ and Maven.

```bash
mvn package
```

This produces `target/fim-java-jar-with-dependencies.jar` (Jackson is
bundled in, so it runs standalone).

## Usage

```bash
# 1. Capture a baseline
java -jar target/fim-java-jar-with-dependencies.jar baseline --dir ./my-app --out baseline.json

# 2. ...later, check for drift
java -jar target/fim-java-jar-with-dependencies.jar check --dir ./my-app --baseline baseline.json
```

Example output after a file is edited and a new file is dropped in:

```
Integrity check: 2 change(s) detected
  + added:    new.txt
  ~ modified: file.txt (25718360e05d... -> 92e78d0b0329...)
```

Clean run:

```
No changes detected. Integrity OK.
```

### Watch mode

```bash
java -jar target/fim-java-jar-with-dependencies.jar watch --dir ./my-app --baseline baseline.json --interval-seconds 30
```

Polls every 30 seconds and only prints when something changes.

### Options

| Command | Flags |
|---|---|
| `baseline` | `--dir <path>` (required), `--out <file>` (default `baseline.json`) |
| `check` | `--dir <path>`, `--baseline <file>` (both required), `--json` |
| `watch` | `--dir <path>`, `--baseline <file>` (both required), `--interval-seconds <n>` (default 30) |

Exit codes for `check`: `0` clean, `2` changes detected, `1` usage error —
easy to wire into a cron job or CI gate.

## Tests

```bash
mvn test
```

7 JUnit 5 tests cover hashing correctness, added/removed/modified
detection, and baseline JSON round-tripping, using JUnit's `@TempDir` so
nothing touches the real filesystem outside a scratch directory.

## Limitations

- Polling-based `watch`, not OS-level file events — fine for periodic
  checks, not meant for sub-second detection.
- No permission/owner/timestamp tracking, only content hashes.
- The baseline file itself isn't signed; protect it the same way you'd
  protect any other security-relevant config (read-only, backed up
  elsewhere).

## License

MIT — see [LICENSE](LICENSE).
