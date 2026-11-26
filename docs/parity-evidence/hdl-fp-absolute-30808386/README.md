# FP Absolute HDL export compared with Evolution (#383)

Mobile exports FP Absolute to VHDL and Verilog as Evolution [ee61bc5d](https://github.com/logisim-evolution/logisim-evolution/commit/ee61bc5d576fb8241f614eb7eb9c487a03215d68) (PR 3174) generates it: the result is the input with the sign bit cleared, so a NaN keeps its payload, and `error` is high when the exponent is all ones and the fraction is not zero. The pinned `d58a61e0` has no HDL for any floating point component; this follows a later Evolution revision.

- Mobile: `30808386302b0060c46574e23982b8b85693d9c1` (`logs/mobile-revision.txt`), clean worktree.
- Evolution: `21f1e15829e550c491d75953f40484be2ed31c66`, `logisim-evolution-5.1.0dev-all.jar` built from that revision with `./gradlew shadowJar`, SHA-256 `806c030e4000809acf1ec4db043d51c027e2f297643e26413808d863898f36d7`.
- Environment: Windows 11, Git Bash, Temurin JDK 21.0.12.1, Icarus Verilog 13.0 and GHDL 6.0.0 (mcode) from MSYS2 UCRT64. NVC, the backend used on macOS, has no MSYS2 build, so `verify.py` ran with `--vhdl-simulator ghdl`.

## Commands

```sh
./gradlew :core:jvmTest --tests '*HdlExportTest' --tests '*FloatingPointLibraryTest' :core:jvmJar :core:jvmTestClasses --rerun
java -cp "core/build/libs/core-jvm.jar;core/build/classes/kotlin/jvm/test;KOTLIN_STDLIB" dev.logisim.mobile.core.HdlExportOracleMainKt OUT/upper upper
python scripts/oracle/hdl-export/verify.py OUT/upper --vhdl-simulator ghdl   # and the same for lower
python scripts/oracle/hdl-export/fp-absolute/run.py --evolution-jar EVOLUTION_JAR --evolution-revision 21f1e158... --out OUT --vhdl-simulator ghdl
```

## Results

| Run | Result |
| --- | --- |
| `HdlExportTest`, `FloatingPointLibraryTest` | Pass ([log](logs/gradle.txt)). The fixtures check every vector against the Mobile circuit simulator first. |
| Mobile export, VHDL keywords in upper case | 142 designs compiled and simulated, 5,024 literal output comparisons ([result](mobile-upper-result.json), [log](logs/mobile-upper.txt)) |
| Mobile export, lower case | 142 designs, 5,024 comparisons ([result](mobile-lower-result.json), [log](logs/mobile-lower.txt)) |
| Evolution's own generator | 8 designs (four widths, two languages), 256 output comparisons ([result](evolution-results.json), [log](logs/evolution.txt)) |

The 142 designs include the 134 of the earlier fixture set and `fp_absolute_8`, `_16`, `_32` and `_64` in both languages, kept in `mobile-upper/` and `mobile-lower/`. Each feeds the seven magnitudes 0, the smallest subnormal, 1.5, 2.0, the largest finite value, infinity and a quiet NaN, with and without the sign bit, and expects the magnitude and `error` high only for the NaN. The expected values are computed from the field layout, not from either generator.

`fp-absolute/run.py` calls `FpAbsoluteHdlGeneratorFactory.getModuleFunctionality` from the Evolution JAR with in-memory preferences, wraps the body in the entity or module Evolution declares (`dataIn`, `result`, `fpError`), and checks it on the same vectors plus a signalling NaN. Both generators therefore produce the same outputs on these inputs. `evolution/` keeps the wrapped designs and benches.

Negative controls: changing one expected `fpError` in the 32-bit Verilog bench and in the 16-bit VHDL bench made Icarus Verilog print `FAIL vector 12` and GHDL stop with `assertion failure ... FAIL vector 12`.

## Difference from the circuit simulator

The Mobile simulator, like Evolution's `Value.createKnown`, stores 32 and 64-bit results through `floatToIntBits` and `doubleToLongBits`, so a NaN becomes `0x7fc00000` or `0x7ff8000000000000`. The generated HDL keeps the payload. The fixtures record both values (`outputs` for the simulator, `hdlOutputs` for the HDL), as for the other hardware-only differences in `docs/hdl-export.md`.
