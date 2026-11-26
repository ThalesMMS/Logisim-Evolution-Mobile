#!/bin/sh
set -eu
cd "$(dirname "$0")/../../.."
# Git Bash on Windows: Java needs ';' between classpath entries and native paths.
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) sep=';' ;; *) sep=':' ;; esac
native() { if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s\n' "$1"; fi; }
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 desktop-logisim.jar [--no-build]" >&2
    exit 2
fi
desktop_jar=$(native "$1")
if [ "${2:-}" != --no-build ]; then ./gradlew :core:jvmJar --console=plain; fi
kotlin_stdlib=${KOTLIN_STDLIB_JAR:-$(python3 - <<'PY'
import os,re
from pathlib import Path
version=re.search(r'kotlin\("multiplatform"\) version "([^"]+)"',Path('build.gradle.kts').read_text()).group(1)
root=Path(os.environ.get('GRADLE_USER_HOME',Path.home()/'.gradle'))
print(next((root/'caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib'/version).glob('*/*.jar')))
PY
)}
kotlin_stdlib=$(native "$kotlin_stdlib")
classes=$(native "$(mktemp -d)")
trap 'rm -rf "$classes"' EXIT HUP INT TERM
out=outputs/ttl-validation
mkdir -p "$out"
javac -cp "$desktop_jar" -d "$classes" scripts/oracle/ttl/TtlDesktopOracle.java
javac -cp "core/build/libs/core-jvm.jar$sep$kotlin_stdlib" -d "$classes" scripts/oracle/ttl/TtlMobileCompare.java
java -Djava.awt.headless=true -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -Djava.util.prefs.userRoot="$classes/prefs" -cp "$classes$sep$desktop_jar" TtlDesktopOracle metadata scripts/oracle/ttl/models.txt > "$out/metadata.tsv"
java -Djava.awt.headless=true -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -Djava.util.prefs.userRoot="$classes/prefs" -cp "$classes$sep$desktop_jar" TtlDesktopOracle vectors scripts/oracle/ttl/combinational-models.txt > "$out/combinational.tsv"
java -Djava.awt.headless=true -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -Djava.util.prefs.userRoot="$classes/prefs" -cp "$classes$sep$desktop_jar" TtlDesktopOracle sequence scripts/oracle/ttl/sequential-models.txt > "$out/sequential.tsv"
java -cp "$classes${sep}core/build/libs/core-jvm.jar$sep$kotlin_stdlib" TtlMobileCompare "$out/combinational.tsv" > "$out/combinational-result.txt"
cat "$out/combinational-result.txt"
java -cp "$classes${sep}core/build/libs/core-jvm.jar$sep$kotlin_stdlib" TtlMobileCompare "$out/sequential.tsv" sequence > "$out/sequential-result.txt"
cat "$out/sequential-result.txt"
