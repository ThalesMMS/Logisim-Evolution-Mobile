# SoC debugger and assemblers after the Nios II div and Nios V cache ports (#372, #373)

- Mobile: `aa38ae686e277af510eedccdebdcc95518d1fa6b` (`core-jvm.jar` SHA-256 `1d5185ccd748fec24faa20c0dbdd77fb7325dcd9e6677fccc7dd0397b23a2497`, built from a clean `core/` tree)
- Evolution: `d58a61e0c290361722b43df5b1a6b86c1f7c7f0d` (`logisim-evolution-5.1.0dev-all.jar` SHA-256 `d0b287d1695bc12f276f42b8db39af9a00e266ce0e0b53472b5aeab795a7a038`)
- Supplementary Evolution JAR: the same d58a61e0 JAR with the three SoC classes that changed up to upstream `main` `c5d1483506f0bb1a2f810a35f9c1ac36299f1f84` compiled in (`Nios2ArithmeticAndLogicalInstructions`, `RV32imAssembler`, `RV32imNiosVVendorSpecific`), SHA-256 `5b2017ab6906901164036c7343bd5db569a6dfa233469c99c2c2a419aa8bb0b5`. These are the only `soc/nios2` and `soc/rv32im` changes between d58a61e0 and c5d14835.
- Environment: macOS 27.2.1 arm64, OpenJDK 21.0.13 (`scripts/java-home.sh`), AWT without a visible window (`apple.awt.UIElement`). JVM only.

## Command

```sh
./gradlew :core:jvmJar
scripts/oracle/soc-debugger/run.sh EVOLUTION_JAR docs/parity-evidence/soc-debugger-aa38ae68/logs

# Supplementary JAR, from an Evolution checkout at c5d14835:
mkdir -p soc-src/com/cburch/logisim/soc/nios2 soc-src/com/cburch/logisim/soc/rv32im soc-classes
cp src/main/java/com/cburch/logisim/soc/nios2/Nios2ArithmeticAndLogicalInstructions.java soc-src/com/cburch/logisim/soc/nios2/
cp src/main/java/com/cburch/logisim/soc/rv32im/{RV32imAssembler,RV32imNiosVVendorSpecific}.java soc-src/com/cburch/logisim/soc/rv32im/
javac -nowarn -cp EVOLUTION_JAR -d soc-classes $(find soc-src -name '*.java')
cp EVOLUTION_JAR logisim-evolution-d58a61e0-soc-c5d14835.jar
(cd soc-classes && jar uf ../logisim-evolution-d58a61e0-soc-c5d14835.jar $(find . -name '*.class'))
scripts/oracle/soc-debugger/run.sh logisim-evolution-d58a61e0-soc-c5d14835.jar docs/parity-evidence/soc-debugger-aa38ae68/logs-c5d14835-soc
```

## Result

Against the pinned JAR: `SoC debugger: 79 of 82 comparisons equal` ([results](logs/soc-debugger-results.json), [log](logs/soc-debugger.txt), [run log](logs/soc-debugger-run.txt)). The three differences are the ones accepted in `parity/matrix.json`, and the comparisons from the `soc-debugger-f0f47b6f` receipt are otherwise unchanged:

- `nios2 asm statements encoded identically`: 110 same, 1 different. `div r2, r3, r4` is `1904c03a` (opx 0x18) in d58a61e0 and `1905283a` (opx 0x25) in Mobile, as in Evolution 3f7f6766 (#372).
- `asm statements accepted by both`: the 8 Nios V cache statements (`cbo.inval.ix`, `cbo.clean.ix`, `cbo.flush.ix`, `cbo.clean`, `cbo.flush`, `cbo.inval`, `cbo.zero`, `fence.i`, each with `x5`) are accepted only by Mobile; d58a61e0 rejects them (#373). 10 statements are rejected by both.
- `rv32 trace text for 48056 decoded words`: 48053 same, 3 different. The three random MISC-MEM words `000d100f`, `001ca00f` and `001fa00f` decode as `fence.i   s10`, `cbo.clean s9` and `cbo.clean t6` in Mobile and as nothing in d58a61e0.

Against the supplementary JAR: `SoC debugger: 81 of 82 comparisons equal` ([results](logs-c5d14835-soc/soc-debugger-results.json), [log](logs-c5d14835-soc/soc-debugger.txt), [run log](logs-c5d14835-soc/soc-debugger-run.txt)). The `div` word and all 48056 RV32 trace texts match. The remaining difference is Evolution's: its assembler throws `NullPointerException: Cannot load from object array because "bytes" is null` on each cache statement, because `RV32imNiosVVendorSpecific.setAsmInstruction` never sets `valid` or the instruction bytes. Mobile emits the MISC-MEM I-type word (rd = 0, funct3 = 2 with the cbo code, or funct3 = 1 with immediate 0 for `fence.i`) that Evolution's decoder accepts.

The focused run `./gradlew :core:jvmTest --tests '*Rv32CpuTest' --tests '*Rv32AssemblerTest' --tests '*Rv32DesktopOracleTest' --tests '*Nios2CpuTest' --tests '*SocProgramDesktopParityTest'` passed at aa38ae68 (39 tests, 0 failures: `Nios2CpuTest` 16, `Rv32CpuTest` 12, `Rv32AssemblerTest` 6, `SocProgramDesktopParityTest` 4, `Rv32DesktopOracleTest` 1).
