# TTL model validation

`TtlComponents.definitions` exports the 85 device types in the desktop `TtlLibrary` catalog. `TtlLogic` evaluates physical, one-based pin numbers. The package geometry and pin labels come from metadata extracted from the desktop JAR.

## Evidence

- 37 JVM tests cover catalog, geometry, registry definitions, combinational behavior, sequential transitions, and all 85 per-model fixtures.
- The desktop oracle checks 444,000 combinational output values and delays, plus 95,232 sequential output values and delays. Both comparisons completed with zero mismatches against Evolution `21f1e158` ([evidence](parity-evidence/ttl-evolution-21f1e158/README.md)), whose 74266 and 7476 pinouts Mobile follows (see [Pinouts after the pin](#pinouts-after-the-pin)).
- The sequential oracle supplies output feedback between steps and injects a deterministic clock for 74123.
- `scripts/oracle/ttl/run.sh` reproduces the differential checks against a supplied desktop JAR.
- `scripts/oracle/ttl/run_internal.sh` compares the internal structure with Evolution's drawing: 85 models with ANSI gates, 30 with IEC gates and 18 with register state, all matching Evolution `21f1e158` ([evidence](parity-evidence/ttl-evolution-21f1e158/README.md); the earlier run against `d58a61e0` is in [ttl-internal-structure-bd0e4e77](parity-evidence/ttl-internal-structure-bd0e4e77/README.md)). `TtlInternalStateTest` checks the value dots of 12 models and the 74123 pulse colour after each of the 9,216 fixture rows.

## Model rules

Power terminals are hidden unless `VccGndPorts=true`. An invalid explicit supply releases normal outputs, as in the desktop implementation. Bidirectional ports retain the previously driven level where the desktop leaves them untouched. Unknown and error inputs follow each model, including Boolean-only desktop models that treat a non-high input as low.

The 74123 exposes `1Rext`, `1Cext`, `2Rext`, and `2Cext`, and requests its next simulation wakeup. The simulator must pass elapsed active propagation time, freezing it while propagation is paused. The 74299 data terminals are bidirectional in the mobile layout so the register can sample external signals and release its shared bus.

`ShowInternalStructure` draws the logic diagram that Evolution paints, described under [Internal structure](#internal-structure). The runtime does not model analog voltage thresholds or electrical drive strength.

The active catalog includes `74283`. It does not register `7483`; these part numbers are not pin-compatible aliases.

### Pinouts after the pin

Evolution corrected two pinouts after `d58a61e0`, and Mobile follows both since #381:

- 74266 ([PR 3209](https://github.com/logisim-evolution/logisim-evolution/pull/3209)): outputs 1Y, 2Y, 3Y and 4Y on pins 3, 4, 10 and 11, as in the SN74HC266 datasheet. The pin had them on 3, 6, 8 and 11 like the 7486, so the second gate read pins 4 and 5 and the third pins 9 and 10. The pins are now named 1A to 4B.
- 7476 ([PR 3201](https://github.com/logisim-evolution/logisim-evolution/pull/3201)): VCC on pin 5 and GND on pin 13, CLK2, nPRE2 and nCLR2 on 6, 7 and 8, nQ1, Q1 and K1 on 14, 15 and 16. The pin had VCC on 16, GND on 8 and the second clock on 5.

A circuit saved with the old pinout keeps its wires where they were, and they now meet other pins. Evolution `5.1.0dev` writes the same file version before and after the change, so neither Evolution nor Mobile can tell such a file apart, and neither migrates it. Rewire a 74266 or 7476 from an older circuit to the new pins. The 74194 pins are also renamed after the datasheet (MR, DSR, D0 to D3, DSL, S0, S1, CP, Q3 to Q0), without changing their position.

## Catalog

| Model | Pins | GND | VCC | Output pins | Bidirectional pins |
| --- | ---: | ---: | ---: | --- | --- |
| 7400 | 14 | 7 | 14 | 3,6,8,11 | None |
| 7402 | 14 | 7 | 14 | 1,4,10,13 | None |
| 7404 | 14 | 7 | 14 | 2,4,6,8,10,12 | None |
| 7405 | 14 | 7 | 14 | 2,4,6,8,10,12 | None |
| 7408 | 14 | 7 | 14 | 3,6,8,11 | None |
| 7410 | 14 | 7 | 14 | 6,8,12 | None |
| 7411 | 14 | 7 | 14 | 6,8,12 | None |
| 7413 | 14 | 7 | 14 | 6,8 | None |
| 7414 | 14 | 7 | 14 | 2,4,6,8,10,12 | None |
| 7418 | 14 | 7 | 14 | 6,8 | None |
| 7419 | 14 | 7 | 14 | 2,4,6,8,10,12 | None |
| 7420 | 14 | 7 | 14 | 6,8 | None |
| 7421 | 14 | 7 | 14 | 6,8 | None |
| 7424 | 14 | 7 | 14 | 3,6,8,11 | None |
| 7427 | 14 | 7 | 14 | 6,8,12 | None |
| 7430 | 14 | 7 | 14 | 8 | None |
| 7432 | 14 | 7 | 14 | 3,6,8,11 | None |
| 7434 | 14 | 7 | 14 | 2,4,6,8,10,12 | None |
| 7436 | 14 | 7 | 14 | 3,6,8,11 | None |
| 7438 | 14 | 7 | 14 | 3,6,8,11 | None |
| 7442 | 16 | 8 | 16 | 1,2,3,4,5,6,7,9,10,11 | None |
| 7443 | 16 | 8 | 16 | 1,2,3,4,5,6,7,9,10,11 | None |
| 7444 | 16 | 8 | 16 | 1,2,3,4,5,6,7,9,10,11 | None |
| 7447 | 16 | 8 | 16 | 9,10,11,12,13,14,15 | 4 |
| 7451 | 14 | 7 | 14 | 6,8 | None |
| 7454 | 14 | 7 | 14 | 8 | None |
| 7458 | 14 | 7 | 14 | 6,8 | None |
| 7464 | 14 | 7 | 14 | 8 | None |
| 7474 | 14 | 7 | 14 | 5,6,8,9 | None |
| 7476 | 16 | 13 | 5 | 10,11,14,15 | None |
| 7485 | 16 | 8 | 16 | 5,6,7 | None |
| 7486 | 14 | 7 | 14 | 3,6,8,11 | None |
| 7487 | 14 | 7 | 14 | 3,6,9,12 | None |
| 7493 | 14 | 10 | 5 | 8,9,11,12 | None |
| 74107 | 14 | 7 | 14 | 2,3,5,6 | None |
| 74123 | 16 | 8 | 16 | 4,5,12,13 | None |
| 74125 | 14 | 7 | 14 | 3,6,8,11 | None |
| 74132 | 14 | 7 | 14 | 3,6,8,11 | None |
| 74138 | 16 | 8 | 16 | 7,9,10,11,12,13,14,15 | None |
| 74139 | 16 | 8 | 16 | 4,5,6,7,9,10,11,12 | None |
| 74148 | 16 | 8 | 16 | 6,7,9,14,15 | None |
| 74151 | 16 | 8 | 16 | 5,6 | None |
| 74153 | 16 | 8 | 16 | 7,9 | None |
| 74157 | 16 | 8 | 16 | 4,7,9,12 | None |
| 74158 | 16 | 8 | 16 | 4,7,9,12 | None |
| 74161 | 16 | 8 | 16 | 11,12,13,14,15 | None |
| 74163 | 16 | 8 | 16 | 11,12,13,14,15 | None |
| 74164 | 14 | 7 | 14 | 3,4,5,6,10,11,12,13 | None |
| 74165 | 16 | 8 | 16 | 7,9 | None |
| 74166 | 16 | 8 | 16 | 13 | None |
| 74173 | 16 | 8 | 16 | 3,4,5,6 | None |
| 74175 | 16 | 8 | 16 | 2,3,6,7,10,11,14,15 | None |
| 74181 | 24 | 12 | 24 | 9,10,11,13,14,15,16,17 | None |
| 74182 | 16 | 8 | 16 | 7,9,10,11,12 | None |
| 74190 | 16 | 8 | 16 | 2,3,6,7,12,13 | None |
| 74192 | 16 | 8 | 16 | 2,3,6,7,12,13 | None |
| 74193 | 16 | 8 | 16 | 2,3,6,7,12,13 | None |
| 74194 | 16 | 8 | 16 | 12,13,14,15 | None |
| 74240 | 20 | 10 | 20 | 3,5,7,9,12,14,16,18 | None |
| 74241 | 20 | 10 | 20 | 3,5,7,9,12,14,16,18 | None |
| 74244 | 20 | 10 | 20 | 3,5,7,9,12,14,16,18 | None |
| 74245 | 20 | 10 | 20 | None | 2,3,4,5,6,7,8,9,11,12,13,14,15,16,17,18 |
| 74257 | 16 | 8 | 16 | 4,7,9,12 | None |
| 74259 | 16 | 8 | 16 | 4,5,6,7,9,10,11,12 | None |
| 74266 | 14 | 7 | 14 | 3,4,10,11 | None |
| 74273 | 20 | 10 | 20 | 2,5,6,9,12,15,16,19 | None |
| 74283 | 16 | 8 | 16 | 1,4,9,10,13 | None |
| 74299 | 20 | 10 | 20 | 4,5,6,7,8,13,14,15,16,17 | None |
| 74374 | 20 | 10 | 20 | 2,5,6,9,12,15,16,19 | None |
| 74365 | 16 | 8 | 16 | 3,5,7,9,11,13 | None |
| 74377 | 20 | 10 | 20 | 2,5,6,9,12,15,16,19 | None |
| 74381 | 20 | 10 | 20 | 8,9,11,12,13,14 | None |
| 74393 | 14 | 7 | 14 | 3,4,5,6,8,9,10,11 | None |
| 74390 | 16 | 8 | 16 | 3,5,6,7,9,10,11,13 | None |
| 74541 | 20 | 10 | 20 | 11,12,13,14,15,16,17,18 | None |
| 74594 | 16 | 8 | 16 | 1,2,3,4,5,6,7,9,15 | None |
| 74595 | 16 | 8 | 16 | 1,2,3,4,5,6,7,9,15 | None |
| 74640 | 20 | 10 | 20 | None | 2,3,4,5,6,7,8,9,11,12,13,14,15,16,17,18 |
| 74670 | 16 | 8 | 16 | 6,7,9,10 | None |
| 74688 | 20 | 10 | 20 | 19 | None |
| 744017 | 16 | 8 | 16 | 1,2,3,4,5,6,7,9,10,11,12 | None |
| 744060 | 16 | 8 | 16 | 1,2,3,4,5,6,7,13,14,15 | None |
| 744094 | 16 | 8 | 16 | 4,5,6,7,9,10,11,12,13,14 | None |
| 744075 | 14 | 7 | 14 | 6,9,10 | None |
| 747266 | 14 | 7 | 14 | 3,6,8,11 | None |

## Internal structure

With `ShowInternalStructure=true`, Evolution's `AbstractTtlGate.paintInstance` calls `paintInternalBase`, which draws the pin outlines, the package outline and notch, the Vcc and GND labels, and then each model's `paintInternal`: gates wired to the pins, flip-flops, signal names, and the register value dots of some sequential models (#374).

Mobile does not port the 68 Java painters by hand. `scripts/oracle/ttl/TtlInternalStructureOracle.java` calls `paintInternalBase` of every desktop factory (facing east, location 0,0, no circuit state) through a recording `Graphics2D` and writes the calls as a display list, once with shaped (ANSI) gates and once with rectangular (IEC) gates. `scripts/oracle/ttl/generate_internal.py` turns the lists into `TtlInternalStructureData.kt`, which `TtlInternalStructure` parses into `VisualPrimitive`s and `TtlRenderer` draws with `drawVisualScene`, rotated with the package.

- The gate shape follows the editor preference. IEC uses Evolution's rectangular gates; ANSI and DIN 40700 use its shaped gates, as `Drawgates` does for every value other than `rectangular`. 30 models draw differently in IEC.
- Evolution's component colour (opaque black) becomes the editor's component colour, so the diagram follows the light and dark themes. The body is not filled, as in Evolution.
- Text keeps Evolution's anchor: the centre of the desktop glyph run, on the desktop baseline. Mobile draws it with its own monospace or sans-serif font at the recorded size.
- 12 models paint register value dots from their state (`drawState`, `structureDot`, `drawBit`). The 7476, 7493, 74161, 74163 and 74390 painted them at `d58a61e0`; their `21f1e158` drawings no longer do. The oracle records their positions separately, and `TtlInternalStructure` binds each dot to a bit of the Mobile `TtlState`. Dots use `Value.getColor()` and the value characters. The 74164, 74165 and 74166 create their register while painting, so they show eight LOW dots before the first propagation, as in Evolution. The printer view keeps them, since the desktop painters ignore it.
- The 74123 prints the pulse width `0.45 · Rext · Cext` of each half from its attributes (`Ttl74123.formatWidth`) and colours it with `Value.TRUE` while that half is in its pulse.
- The pin labels of the package view are not drawn on top of the diagram. The diagram carries Evolution's own pin labels: the Vcc and GND labels, and the signal names of the 45 models whose `paintInternal` writes them (`paintPortNames`, `paintPortNamesByPin` or explicit text). The inspector and the port tooltips still give every pin name.

### Inventory

Counts at Evolution `d58a61e0` (the pin): 69 classes declare `paintInternal`, the abstract `AbstractTtlGate` and 68 painters (66 `Ttl*` classes, `AbstractOctalBuffers` and `AbstractOctalFlops`). Through inheritance they draw all 85 models of the active catalog; 19 models inherit their painter, such as 74132 from `Ttl7400` and 74273 from `AbstractOctalFlops`. Evolution `main` at `21f1e158` keeps the same 85 models and 68 painters and redraws 14 of them (15 models, since `Ttl74161` also draws the 74163). Two of those changes also correct a pinout (see [Pinouts after the pin](#pinouts-after-the-pin)). Mobile's `TtlCatalog` has the same 85 models; it draws the 15 redrawn models from a `21f1e158` recording and the other 70 from the pinned one (#382).

`TtlInternalStructureData.kt` merges two recordings: `REDRAWN="7447 7476 7485 7487 7493 74123 74138 74161 74163 74194 74266 74283 74365 74390 744017" EVOLUTION_REVISION=21f1e158 scripts/oracle/ttl/run_internal.sh JAR` regenerates only those models from a `21f1e158` JAR and keeps the committed entries of the others (`generate_internal.py --base --only`). Text anchors come from the recording platform's Java font metrics: the 70 pinned models were recorded on macOS, the 15 redrawn ones on Windows with JDK 21.0.12, where a centred label can sit half a unit away from the macOS anchor. A run on macOS therefore reports these 15 entries as stale; regenerate them there with `UPDATE=1` to keep one platform. `docs/parity-evidence/ttl-internal-structure-bd0e4e77/evolution-upstream-c5d14835/` holds four of the later drawings as #374 recorded them before the change.

"Recorded operations" counts the shaped drawing, value dots excluded. "Gates placed by AbstractTtlGate" is `numberOfGatesToDraw` (the `drawGates` constructor flag), with one `paintInternal` call per gate.

| Model | paintInternal at d58a61e0 | Gates placed by AbstractTtlGate | Recorded operations | IEC drawing | Value dots | Evolution 21f1e158 | Mobile |
| --- | --- | ---: | ---: | --- | --- | --- | --- |
| 7400 | `Ttl7400` | 4 | 42 | Differs | — | Unchanged | Drawn |
| 7402 | `Ttl7402` | 4 | 46 | Differs | — | Unchanged | Drawn |
| 7404 | `Ttl7404` | 6 | 42 | Differs | — | Unchanged | Drawn |
| 7405 | `Ttl7405` | 6 | 54 | Differs | — | Unchanged | Drawn |
| 7408 | `Ttl7408` | 4 | 38 | Differs | — | Unchanged | Drawn |
| 7410 | `Ttl7410` | — | 39 | Differs | — | Unchanged | Drawn |
| 7411 | `Ttl7410` | — | 36 | Differs | — | Unchanged | Drawn |
| 7413 | `Ttl7413` | — | 44 | Differs | — | Unchanged | Drawn |
| 7414 | `Ttl7404` | 6 | 42 | Differs | — | Unchanged | Drawn |
| 7418 | `Ttl7413` | — | 44 | Differs | — | Unchanged | Drawn |
| 7419 | `Ttl7404` | 6 | 42 | Differs | — | Unchanged | Drawn |
| 7420 | `Ttl7413` | — | 44 | Differs | — | Unchanged | Drawn |
| 7421 | `Ttl7413` | — | 42 | Differs | — | Unchanged | Drawn |
| 7424 | `Ttl7400` | 4 | 42 | Differs | — | Unchanged | Drawn |
| 7427 | `Ttl7410` | — | 42 | Differs | — | Unchanged | Drawn |
| 7430 | `Ttl7430` | — | 40 | Differs | — | Unchanged | Drawn |
| 7432 | `Ttl7432` | 4 | 42 | Differs | — | Unchanged | Drawn |
| 7434 | `Ttl7434` | 6 | 36 | Same | — | Unchanged | Drawn |
| 7436 | `Ttl7436` | 4 | 46 | Differs | — | Unchanged | Drawn |
| 7438 | `Ttl7438` | 4 | 114 | Differs | — | Unchanged | Drawn |
| 7442 | `Ttl7442` | — | 79 | Same | — | Unchanged | Drawn |
| 7443 | `Ttl7442` | — | 79 | Same | — | Unchanged | Drawn |
| 7444 | `Ttl7442` | — | 79 | Same | — | Unchanged | Drawn |
| 7447 | `Ttl7447` | — | 119 | Same | — | Redrawn (logisim-evolution#3170) | Drawn from 21f1e158 |
| 7451 | `Ttl7451` | — | 48 | Differs | — | Unchanged | Drawn |
| 7454 | `Ttl7454` | — | 43 | Differs | — | Unchanged | Drawn |
| 7458 | `Ttl7458` | — | 51 | Differs | — | Unchanged | Drawn |
| 7464 | `Ttl7464` | — | 47 | Differs | — | Unchanged | Drawn |
| 7474 | `Ttl7474` | — | 78 | Same | 2 | Unchanged | Drawn |
| 7476 | `Ttl7476` | — | 72 | Same | — | Redrawn (logisim-evolution#3201) | Drawn from 21f1e158 |
| 7485 | `Ttl7485` | — | 76 | Same | — | Redrawn (logisim-evolution#3171) | Drawn from 21f1e158 |
| 7486 | `Ttl7486` | 4 | 46 | Differs | — | Unchanged | Drawn |
| 7487 | `Ttl7487` | — | 108 | Same | — | Redrawn (logisim-evolution#3172) | Drawn from 21f1e158 |
| 7493 | `Ttl7493` | — | 86 | Differs | — | Redrawn (logisim-evolution#3197) | Drawn from 21f1e158 |
| 74107 | `Ttl74107` | — | 60 | Same | 2 | Unchanged | Drawn |
| 74123 | `Ttl74123` | — | 74 | Same | Pulse width colour | Redrawn (logisim-evolution#3200) | Drawn from 21f1e158 |
| 74125 | `Ttl74125` | 4 | 42 | Same | — | Unchanged | Drawn |
| 74132 | `Ttl7400` | 4 | 42 | Differs | — | Unchanged | Drawn |
| 74138 | `Ttl74138` | — | 131 | Same | — | Redrawn (logisim-evolution#3208) | Drawn from 21f1e158 |
| 74139 | `Ttl74139` | — | 36 | Same | — | Unchanged | Drawn |
| 74148 | `Ttl74148` | — | 36 | Same | — | Unchanged | Drawn |
| 74151 | `Ttl74151` | — | 49 | Same | — | Unchanged | Drawn |
| 74153 | `Ttl74153` | — | 55 | Same | — | Unchanged | Drawn |
| 74157 | `Ttl74157` | — | 36 | Same | — | Unchanged | Drawn |
| 74158 | `Ttl74157` | — | 36 | Same | — | Unchanged | Drawn |
| 74161 | `Ttl74161` | — | 63 | Same | — | Redrawn (logisim-evolution#3220) | Drawn from 21f1e158 |
| 74163 | `Ttl74161` | — | 63 | Same | — | Redrawn (logisim-evolution#3220) | Drawn from 21f1e158 |
| 74164 | `Ttl74164` | — | 31 | Same | 8 | Unchanged | Drawn |
| 74165 | `Ttl74165` | — | 35 | Same | 8 | Unchanged | Drawn |
| 74166 | `Ttl74166` | — | 35 | Same | 8 | Unchanged | Drawn |
| 74173 | `Ttl74173` | — | 36 | Same | — | Unchanged | Drawn |
| 74175 | `Ttl74175` | — | 92 | Same | — | Unchanged | Drawn |
| 74181 | `Ttl74181` | — | 52 | Same | — | Unchanged | Drawn |
| 74182 | `Ttl74182` | — | 36 | Same | — | Unchanged | Drawn |
| 74190 | `Ttl74190` | — | 105 | Same | — | Unchanged | Drawn |
| 74192 | `Ttl74192` | — | 35 | Same | 4 | Unchanged | Drawn |
| 74193 | `Ttl74192` | — | 35 | Same | 4 | Unchanged | Drawn |
| 74194 | `Ttl74194` | — | 52 | Same | — | Redrawn (logisim-evolution#3210) | Drawn from 21f1e158 |
| 74240 | `AbstractOctalBuffers` | — | 78 | Same | — | Unchanged | Drawn |
| 74241 | `AbstractOctalBuffers` | — | 69 | Same | — | Unchanged | Drawn |
| 74244 | `AbstractOctalBuffers` | — | 70 | Same | — | Unchanged | Drawn |
| 74245 | `Ttl74245` | — | 133 | Same | — | Unchanged | Drawn |
| 74257 | `Ttl74257` | — | 63 | Same | — | Unchanged | Drawn |
| 74259 | `Ttl74259` | — | 67 | Same | 1 | Unchanged | Drawn |
| 74266 | `Ttl74266` | — | 78 | Differs | — | Redrawn (logisim-evolution#3209) | Drawn from 21f1e158 |
| 74273 | `AbstractOctalFlops` | — | 127 | Same | 8 | Unchanged | Drawn |
| 74283 | `Ttl74283` | — | 76 | Same | — | Redrawn (logisim-evolution#3202) | Drawn from 21f1e158 |
| 74299 | `Ttl74299` | — | 44 | Same | — | Unchanged | Drawn |
| 74374 | `Ttl74374` | — | 99 | Same | — | Unchanged | Drawn |
| 74365 | `Ttl74365` | — | 73 | Differs | — | Redrawn (logisim-evolution#3207) | Drawn from 21f1e158 |
| 74377 | `AbstractOctalFlops` | — | 127 | Same | 8 | Unchanged | Drawn |
| 74381 | `Ttl74381` | — | 44 | Same | — | Unchanged | Drawn |
| 74393 | `Ttl74393` | — | 116 | Same | 8 | Unchanged | Drawn |
| 74390 | `Ttl74390` | — | 76 | Same | — | Redrawn (logisim-evolution#3203) | Drawn from 21f1e158 |
| 74541 | `Ttl74541` | — | 63 | Same | — | Unchanged | Drawn |
| 74594 | `Ttl74594` | — | 54 | Same | — | Unchanged | Drawn |
| 74595 | `Ttl74595` | — | 100 | Same | — | Unchanged | Drawn |
| 74640 | `Ttl74640` | — | 149 | Same | — | Unchanged | Drawn |
| 74670 | `Ttl74670` | — | 36 | Same | — | Unchanged | Drawn |
| 74688 | `Ttl74688` | — | 125 | Same | — | Unchanged | Drawn |
| 744017 | `Ttl744017` | — | 48 | Same | — | Redrawn (logisim-evolution#3212) | Drawn from 21f1e158 |
| 744060 | `Ttl744060` | — | 35 | Same | 14 | Unchanged | Drawn |
| 744094 | `Ttl744094` | — | 98 | Same | — | Unchanged | Drawn |
| 744075 | `Ttl744075` | — | 39 | Differs | — | Unchanged | Drawn |
| 747266 | `Ttl747266` | 4 | 50 | Differs | — | Unchanged | Drawn |
