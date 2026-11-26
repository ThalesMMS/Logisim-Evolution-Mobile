#!/bin/sh
# Compares the TTL internal structure (ShowInternalStructure=true) with the desktop drawing (#374).
# Usage: [EVOLUTION_REVISION=sha] [UPDATE=1] scripts/oracle/ttl/run_internal.sh desktop-logisim.jar [--no-build]
# EVOLUTION_REVISION names the JAR's revision in the generated header; UPDATE=1 rewrites the committed data.
# REDRAWN="7447 7476 ..." regenerates only those models and keeps the committed entries of the others.
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
out=outputs/ttl-internal-structure
classes=$(native "$(mktemp -d)")
generated=$(native "$(mktemp -d)")
trap 'rm -rf "$classes" "$generated"' EXIT HUP INT TERM
rm -rf "$out"
mkdir -p "$out/evolution" "$out/compare"
models=$(cat scripts/oracle/ttl/models.txt)
stateful="7474 7476 7493 74107 74123 74161 74163 74164 74165 74166 74192 74193 74259 74273 74377 74390 74393 744060"

# 1. Evolution: display lists and rasters of every model, in both gate shapes, and the state replay.
javac -cp "$desktop_jar" -d "$classes" scripts/oracle/ttl/TtlDesktopOracle.java scripts/oracle/ttl/TtlInternalStructureOracle.java
javac -d "$classes" scripts/oracle/ttl/TtlInternalStructureCompare.java
run_oracle() {
    java -Djava.awt.headless=true -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -Djava.util.prefs.userRoot="$classes/prefs" -cp "$classes$sep$desktop_jar" TtlInternalStructureOracle "$@"
}
run_oracle scripts/oracle/ttl/models.txt "$out/evolution" shaped 4
run_oracle scripts/oracle/ttl/models.txt "$out/evolution" rectangular 4
# shellcheck disable=SC2086
run_oracle --state core/src/jvmTest/resources/ttl-fixtures "$out/evolution/state.txt" $stateful

# 2. The committed data must be what the generator makes from these recordings.
mkdir -p "$generated/core/src/commonMain/kotlin/dev/logisim/mobile/core" "$generated/core/src/jvmTest/resources"
source="Evolution ${EVOLUTION_REVISION:-d58a61e0}, logisim-evolution-5.1.0dev-all.jar SHA-256 $(shasum -a 256 "$desktop_jar" | cut -d' ' -f1)"
if [ -n "${REDRAWN:-}" ]; then
    # Only these models come from this recording; the others keep the committed entries, whose text
    # anchors depend on the recording platform's Java fonts (#382).
    cp core/src/commonMain/kotlin/dev/logisim/mobile/core/TtlInternalStructureData.kt "$generated/base.kt"
    python3 -I scripts/oracle/ttl/generate_internal.py "$out/evolution/shaped.txt" "$out/evolution/rectangular.txt" \
        --state "$out/evolution/state.txt" --root "$generated" --source "$source" \
        --base "$generated/base.kt" --only "$REDRAWN"
else
    python3 -I scripts/oracle/ttl/generate_internal.py "$out/evolution/shaped.txt" "$out/evolution/rectangular.txt" \
        --state "$out/evolution/state.txt" --root "$generated" --source "$source"
fi
if [ "${UPDATE:-0}" = 1 ]; then
    cp "$generated/core/src/commonMain/kotlin/dev/logisim/mobile/core/TtlInternalStructureData.kt" core/src/commonMain/kotlin/dev/logisim/mobile/core/
    cp "$generated/core/src/jvmTest/resources/ttl-internal-state.tsv" core/src/jvmTest/resources/
fi
diff -u core/src/commonMain/kotlin/dev/logisim/mobile/core/TtlInternalStructureData.kt \
    "$generated/core/src/commonMain/kotlin/dev/logisim/mobile/core/TtlInternalStructureData.kt" > "$out/generated.diff" \
    && echo "generated data: up to date" || { echo "generated data: STALE, see $out/generated.diff" >&2; exit 1; }
cmp core/src/jvmTest/resources/ttl-internal-state.tsv "$generated/core/src/jvmTest/resources/ttl-internal-state.tsv"

# 3. Mobile: the same models drawn by the canvas path on the JVM host (Skia).
if [ "${2:-}" != --no-build ]; then
    ./gradlew --console=plain :core:jvmTest --tests '*TtlInternal*' --rerun :composeApp:jvmTest --tests '*TtlInternalStructureHostTest' --rerun
fi
mobile=composeApp/build/ttl-internal-structure

# 4. Raster comparison.
compare() {
    java -Djava.awt.headless=true -cp "$classes" TtlInternalStructureCompare "$@"
}
status=0
# shellcheck disable=SC2086
compare "$out/evolution/shaped.txt" "$out/evolution/shaped" "$mobile/shaped" "$out/compare/shaped" $models || status=1
rectangular=$(ls "$mobile/rectangular" | sed 's/\.png$//')
# shellcheck disable=SC2086
compare "$out/evolution/rectangular.txt" "$out/evolution/rectangular" "$mobile/rectangular" "$out/compare/rectangular" $rectangular || status=1
# shellcheck disable=SC2086
compare "$out/evolution/shaped.txt,$out/evolution/state.txt" "$out/evolution/state" "$mobile/state" "$out/compare/state" $stateful || status=1
for kind in shaped rectangular state; do
    printf '%s: %s\n' "$kind" "$(awk -F'\t' 'NR>1{n++; if($2=="PASS")p++} END{print p"/"n" PASS"}' "$out/compare/$kind/results.tsv")"
done
exit $status
