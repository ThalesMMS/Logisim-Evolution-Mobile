# TTL internal structure compared with Evolution (#374)

Row `behavior/ttl-internal-structure`. With `ShowInternalStructure=true`, every TTL model draws the diagram that Evolution's `AbstractTtlGate.paintInternalBase` paints: pin outlines, package outline, Vcc and GND labels, gates wired to the pins, flip-flops, signal names, and the register value dots of the sequential models. [`docs/ttl-validation.md`](../../ttl-validation.md#internal-structure) describes the design and lists the inventory.

- Mobile: `bd0e4e77`, clean worktree. The rasters come from the `composeApp` JVM host target, drawn by `drawClassicComponent` (or by `drawTtlPackage` with a replayed `TtlState`) through Skia, as on iOS.
- Evolution: JAR `d0b287d1695bc12f276f42b8db39af9a00e266ce0e0b53472b5aeab795a7a038` (`d58a61e0`), `TtlInternalStructureOracle.java`, `-Djava.awt.headless=true`. The oracle calls `paintInternalBase` of each factory through a recording `Graphics2D` that also paints a `BufferedImage`, so the Evolution PNG is Java2D's own raster of the same calls.
- Environment: macOS 27.2.1 (Apple silicon), Homebrew OpenJDK 21.0.13. JVM and host tests only; no simulator or device was used.

## Commands

```sh
scripts/oracle/ttl/run_internal.sh EVOLUTION_JAR
./gradlew :core:jvmTest --tests '*TtlInternal*' --rerun :composeApp:jvmTest --tests '*TtlInternalStructureHostTest' --rerun
```

`run_internal.sh` records the drawings of all 85 models with shaped (ANSI) and rectangular (IEC) gates, replays the sequential fixtures through the desktop models for the state dots, regenerates `TtlInternalStructureData.kt` and `ttl-internal-state.tsv` into a temporary directory and requires them to equal the committed files, runs the focused tests, and compares the rasters.

## Criterion

`TtlInternalStructureCompare.java` compares the two rasters at scale 4 with a 4-unit margin. Ink is a pixel with luminance below 128.

- Geometry: outside the text boxes, every ink pixel of either image must have ink in the other within 2 pixels (half a circuit unit). Recall and precision must both reach 0.99.
- Text: each text box from the Evolution display list (desktop centre, baseline, advance and size) must hold ink in both images. Glyph shapes are not compared, since Mobile draws with its own fonts.

A negative control swapping the Mobile 7400 and 7408 images fails with geometry recall 0.78 and 0.89.

## Results

| Set | Models | Result | Geometry recall / precision (minimum) | Text boxes with ink in both |
| --- | ---: | --- | --- | --- |
| ANSI gates, no state | 85 | 85 PASS | 1.0000 / 1.0000 | 748 of 748 |
| IEC gates, models whose drawing differs | 28 | 28 PASS | 1.0000 / 1.0000 | 175 of 175 |
| Register state after fixture row 100 | 18 | 18 PASS | 1.0000 / 1.0000 | 329 of 329, value digits included |

`results/*.tsv` lists every model. `TtlInternalStateTest` also checks the dot values of 17 models and the 74123 pulse-width colour after each of the 9,216 fixture rows against the Evolution replay, with no mismatch. `TtlInternalStructureHostTest` renders every model in the light and dark themes (no black stroke on the dark canvas) and four models in all four facings, inside their bounds.

Fidelity: the geometry is identical because it is Evolution's own display list; the differences are the fonts (Mobile's monospace and sans-serif against Java's DialogInput and Dialog, with the same anchors) and anti-aliasing.

## Files

- `composites/{shaped,rectangular,state}/MODEL.png`: Evolution, Mobile, and the overlay (black in both, red Evolution only, blue Mobile only) for a sample of combinational models (7400, 7432, 7442, 7447, 7486, 74138, 74151, 74181, 74245, 74283, 74541), IEC gates (7400, 7432, 7486, 74266) and sequential models with state (7474, 74107, 74123, 74161, 74164, 74273, 74390, 74393, 744060).
- `mobile/dark/`: 7400, 7474 and 74181 on the dark canvas. `mobile/facing/`: 7474 in the four facings.
- `evolution-upstream-c5d14835/`: 7447, 7485, 7493 and 74138 as Evolution `c5d14835` draws them, from the 13 redrawn classes compiled against the pinned JAR, with their operation counts. These later diagrams are outside the pinned reference and wait for a change of the pin.
- `results/`: the comparison TSVs.

## Open items

- The 13 diagrams Evolution redrew after the pin (7447, 7476, 7485, 7487, 7493, 74123, 74138, 74194, 74266, 74283, 74365, 74390, 744017) follow `d58a61e0` until the pin changes.
- `TtlComponents.kt` gained the `ttlState` accessor the dots need, so the matrix flags the 85 sweep-based `TTL/*` rows for recheck. The simulation code is unchanged: `TtlFixtureTest`, `TtlSequentialTest`, `TtlCombinationalTest` and `TtlDefinitionTest` (36 tests) pass at `bd0e4e77`. Clearing the flag needs a new temporal sweep receipt.
