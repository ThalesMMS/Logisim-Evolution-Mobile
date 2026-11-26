# Library rows rechecked at Mobile 434c72f2 (2026-11-23)

For #377, these runs repeat the recorded comparisons of the library rows flagged **Needs recheck** at `434c72f2`. There were 175 flagged rows, 85 of them `TTL/*`, which were flagged only because #374 added the `ttlState` accessor to `TtlComponents.kt`. Product rows are rechecked separately.

- Mobile: `434c72f2f27d3708635f9e6db9ac21f13e74981b` (`main`), clean worktree with no changes before the runs. `core-jvm.jar` (`./gradlew :core:jvmJar`) SHA-256 `6f2ba0cffdf59c73f686ea1970a7e9d53ee437e723f753444e382a931fd2b6b9`.
- Evolution: `d58a61e0c290361722b43df5b1a6b86c1f7c7f0d`, `logisim-evolution-5.1.0dev-all.jar` SHA-256 `d0b287d1695bc12f276f42b8db39af9a00e266ce0e0b53472b5aeab795a7a038`. The hash was checked before use.
- Environment: macOS 27.2 (Apple silicon), Homebrew OpenJDK 21.0.13. Runs were JVM only: no emulator, simulator, or device. Each run held the shared build lock.

`EVOLUTION_JAR` is the desktop JAR above. In `logs/`, local paths are replaced by `EVOLUTION/`, `MOBILE/` and `WORK/`.

## Temporal sweep and BLIF receipt

```sh
./gradlew :core:jvmJar
python3 scripts/parity_matrix.py reverify temporal-kinds --evolution-jar EVOLUTION_JAR --work WORK/temporal
python3 scripts/parity_matrix.py reverify blif-circuit --evolution-jar EVOLUTION_JAR --work WORK/blif
```

Both commands rewrote their TSVs and receipts (`../temporal-kinds/receipt.json` and `../blif-circuit/receipt.json`, every suite at `434c72f2`). [Runner log](logs/sweep.txt).

| Suite | Fixtures | SAME | VALUE / DIFF | DESKTOP_ERROR |
| --- | ---: | ---: | ---: | ---: |
| default | 160 | 156 | 1 | 3 |
| variants | 984 (7 rejected) | 963 | 9 | 12 |
| negative | 984 (7 rejected) | 967 | 7 | 10 |
| pairs | 556 (4 rejected) | 541 | 5 | 10 |
| roundtrip | 790 | 771 | 19 DIFF | |
| BLIF (filtered suites and corpus) | 14 per suite | all | 0 | `blif_circuit__invalid` in `corpus-roundtrip` (excluded) |

Compared with the TSVs recorded at `2653be42`, 20 results changed and all of them are covered by differences already listed in `parity/matrix.json`:

- 19 `fpround` results went from SAME to VALUE or DIFF: default 1, variants 5, negative 5, pairs 3, and roundtrip 5. The cause is `evolution-fpround-without-long` (accepted): since #369, Mobile follows Evolution 76b178dd, which keeps the sign of zero, NaN, and values beyond the long range. The d58a61e0 JAR saturates them, outputs 0 for NaN, and drops the sign of zero. The earlier TSVs predate #369. Now that the difference has sweep rows, `FPArithmetic/FpRound` moves from Matches Evolution to Explained difference.
- `random__width_1__appearance_logisim_evolution` (pairs) went from SAME to VALUE. The cause is `evolution-wall-clock-seed` (accepted): with seed 0 and an Evolution appearance, Evolution seeds from `System.currentTimeMillis()`. A 1-bit output can match by chance, as it did in the earlier run.

No new divergence appeared. Of the 175 flagged rows, the sweep clears all 134 whose evidence is the sweep: 81 `TTL/*` rows, 51 other components, `HDL-IP/VhdlEntityComponent`, and `HDL-IP/BlifCircuitComponent` (sweep and BLIF receipt).

## Declared oracles

| Evidence | Command | Result | Rows |
| --- | --- | --- | --- |
| `annotations-434c72f2` | `scripts/oracle/annotations/run.sh EVOLUTION_JAR` | PASS: the desktop reopened and saved 9 components, matching Text/Image geometry, embedded RGBA, rotated custom ports, and annotation attributes. [log](logs/annotations.txt) | `Base/Text.FACTORY`, `Base/Image.FACTORY` |
| `base-tools-434c72f2` | `scripts/oracle/base-tools/run.sh EVOLUTION_JAR OUT` | SAME for text-tool (4 components), poke, menu, image, and image-menu: 14 resized and transparent images differ by 0 pixels. [log](logs/base-tools.txt), [text tool](logs/base-tools-text-tool.txt), [image pixels](logs/base-tools-image-pixels.txt) | `Base/TextTool`, `Base/Image.FACTORY` |
| `wiring-434c72f2` | `scripts/verify-wiring-oracle.sh EVOLUTION_JAR` | PASS: 80,000 value comparisons and 533 geometry cases. [log](logs/wiring.txt) | `Wiring/Probe`, `PullResistor`, `PowerOnReset`, `Constant`, `Power`, `Ground`, `DoNotConnect`, `SplitterFactory`, `Tunnel`, `Clock` |
| `io-434c72f2` | `python3 scripts/oracle/io/generate-layout-fixtures.py EVOLUTION_JAR --check`; `scripts/oracle/io/run-state-oracle.sh EVOLUTION_JAR`; `./gradlew :core:jvmTest --tests '*IoGeometryOracleTest'` | PASS: the desktop regenerates the 77 committed geometry cases and the Mobile layout matches them. 889,328 keyboard, TTY, and dot-matrix state observations match with seed 440071. [geometry](logs/io-geometry.txt), [state](logs/io-state.txt), [tests](logs/gradle-tests.txt) | `I/O/Button`, `DipSwitch`, `Joystick`, `Led`, `LedBar`, `RgbLed`, `SevenSegment`, `HexDigit`, `DotMatrix`, `Tty` |
| `extra-434c72f2` | `scripts/oracle/extra/run.sh EVOLUTION_JAR` | PASS: 71,316 comparisons. [log](logs/extra.txt) | `Input/Output-Extra/Slider`, `TwoPinLed` |
| `ttl-434c72f2` | `scripts/oracle/ttl/run.sh EVOLUTION_JAR --no-build` | PASS: 444,000 combinational and 95,232 sequential comparisons with 0 mismatches across 85 models. [log](logs/ttl.txt) | `TTL/Ttl7447`, `Ttl74245`, `Ttl74299`, `Ttl74640` (and the 81 swept TTL rows) |
| `output-devices-434c72f2` | `scripts/oracle/output-devices/run.sh EVOLUTION_JAR` | PASS: 28,631,313 comparisons with seed 317072026, including RTC (802) and SoC VGA (902,423). [log](logs/output-devices.txt) | `I/O/RealTimeClock`, `I/O/Video.factory`, `Input/Output-Extra/Buzzer`, `DigitalOscilloscope`, `Soc/SocVga` |
| `soc-434c72f2` | `javac -cp EVOLUTION_JAR -d CLASSES scripts/SocProgramDesktopOracle.java scripts/SocRv32DesktopOracle.java`; `java -cp CLASSES:EVOLUTION_JAR SocProgramDesktopOracle fixtures/soc/program-{rv32,nios2}.circ`; `java -cp CLASSES:EVOLUTION_JAR SocRv32DesktopOracle`; `./gradlew :core:jvmTest --tests '*SocProgramDesktopParityTest' --tests '*Rv32DesktopOracleTest' --tests '*Rv32CpuTest' --tests '*Rv32AssemblerTest' --tests '*Nios2CpuTest'` | PASS: both desktop program traces are byte-identical to `fixtures/soc/program-*-desktop.csv`, and the 288 RV32 register vectors are identical to those in `Rv32DesktopOracleTest`. 40 tests pass with 0 failures. `SocProgramDesktopOracle` needs a graphics environment because the bus constructs a `JDialog`, so it runs without `-Djava.awt.headless=true`. [rv32](logs/soc-program-rv32-desktop.csv), [nios2](logs/soc-program-nios2-desktop.csv), [vectors](logs/soc-rv32-desktop.csv), [tests](logs/gradle-tests.txt) | `Soc/Rv32imRiscV`, `Soc/SocBus`, `Soc/SocMemory` |
| `soc-debugger-libraries-434c72f2` | `scripts/oracle/soc-debugger/run.sh EVOLUTION_JAR OUT` | 79 of 82 comparisons equal. The 3 differences are the accepted ones recorded at `aa38ae68`: `div r2, r3, r4` (#372), the 8 Nios V cache statements accepted only by Mobile (#373), and 3 MISC-MEM words of the 48,056-word RV32 trace (#373). [log](logs/soc-debugger.txt), [results](logs/soc-debugger-results.json) | `Soc/Rv32imRiscV` |
| `corpus-replay-libraries-434c72f2` | `python3 scripts/oracle-replay-receipt.py --source-root . --mobile-revision 434c72f2… --desktop-revision d58a61e0… --core-jar core-jvm.jar --desktop-jar EVOLUTION_JAR --core-sha256 6f2ba0cf… --desktop-sha256 d0b287d1… --output OUT` | PASS: 7 circuit fixtures produced 222 records with 0 differences, and 6 temporal fixtures (155 records) match in settle and step. `oracle-replay/receipt.json` is the unchanged receipt. The archived `source/` and `inputs/` copies were left out, and their hashes remain in the receipt. [log](logs/corpus-replay.txt) | `Wiring/Pin`, `SplitterFactory`, `Tunnel`, `Clock`, `I/O/HexDigit` |
| `fp-fd5543cb` | `scripts/oracle/floating-point/run.sh EVOLUTION_JAR` at `fd5543cb`, with the `core-jvm.jar` built from a clean `434c72f2` tree (`core/` is identical at both revisions) | PASS: 94,116 outputs/delays and 1,160 layouts against 18 desktop factories with seed 33042026. 569 value mismatches are covered by `evolution-fpround-without-long`, and 0 are unexplained. All 569 are FPRound outputs in the round and truncate modes, and each one equals Evolution 76b178dd's formula applied to the same input. In each one the d58a61e0 JAR outputs 0 (for NaN, which includes unknown or error inputs, or a negative result that rounds to zero) or ±2^63 (`5f000000`/`df000000`, `43e0…`/`c3e0…`, for values beyond the long range). FPConstant (7 literals at each of 4 widths) and the other 17 factories have 0 mismatches. [log](logs/floating-point-fd5543cb.txt) | `FPArithmetic/FpConstant` |

### Floating-point oracle fix (`fd5543cb`)

At `434c72f2`, the committed oracle exits 1 at its first mismatch, `FPRound {fpwidth=8} input=10000000 desktop=00 mobile=10000000` ([historical log](logs/floating-point.txt)). This is the accepted `evolution-fpround-without-long` difference (#369), but the stop prevented the oracle from reaching FPConstant and the remaining cases. Commit `fd5543cb` changes only `scripts/oracle/floating-point/FloatingPointOracle.java` and `docs/floating-point.md`. The oracle now compares every case and lists mismatches under `ACCEPTED <id>`. It accepts a mismatch only when FPRound's round or truncate output equals the later Evolution formula applied to the same input, and it fails on every other mismatch, which it prints as `UNEXPLAINED`. A negative check in a scratch copy replaced the later truncate formula with `Math.floor`. That copy left 129 mismatches unexplained and exited 1, so the classification does not accept an arbitrary FPRound output.

## Tally

Of the 175 flagged library rows, 173 are cleared: the sweep clears 134, and the declared oracles above clear 39. After regenerating, library rows show 197 Matches Evolution and 8 Explained differences, including `FPArithmetic/FpRound`, plus 2 Needs recheck. `python3 scripts/parity_matrix.py --check` passes, and `scripts/tests/test_parity_matrix.py` passes 12 tests.

## Still flagged

The coordinator will handle these rows later, because the iOS simulator is in use by another agent.

| Rows | Recorded evidence | First change | Reason | Resume condition |
| --- | --- | --- | --- | --- |
| `Base/WiringTool`, `Base/EditTool` | `editing-android-2653be42`, `editor-ios-2653be42` | `86cb33ec` (`CircuitEditor.kt` applies `placedAttributes` when placing and pasting) | The evidence is an Android emulator and iOS simulator scenario, and this JVM-only recheck started no emulator or simulator. | Rerun `scripts/e2e-android.py` (editing-create-properties, editing-wires-truth-table, editing-selection-history, editing-viewport-touch-parity) and `scripts/run-ios-e2e.py` `EditorEndToEndTests/testEditingConnectionsHistoryAndViewport` at a `main` revision. |

These runs cover headless JVM behavior only. They do not cover Android or iOS rendering, touch input, sound, or device timing.
