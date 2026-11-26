# Product rows rechecked with the JVM oracles at `434c72f2` (#377)

- Mobile: `434c72f2f27d3708635f9e6db9ac21f13e74981b` (`main`), clean worktree, no code changes before the runs. `core-jvm.jar` SHA-256 `6f2ba0cffdf59c73f686ea1970a7e9d53ee437e723f753444e382a931fd2b6b9`.
- Evolution: `d58a61e0c290361722b43df5b1a6b86c1f7c7f0d`, `logisim-evolution-5.1.0dev-all.jar` SHA-256 `d0b287d1695bc12f276f42b8db39af9a00e266ce0e0b53472b5aeab795a7a038`. The Evolution source checkout next to the JAR was at `c5d14835`, so `integrations/results.json` records that revision under `evolution_revision`. Only the Tcl wrapper is read from the checkout, and it is identical in `d58a61e0` and `c5d14835`. The Evolution JAR is the pinned one.
- Environment: macOS 27.2.1 arm64, Homebrew OpenJDK 21.0.13, Yosys, Icarus Verilog, NVC, and tclsh. Run on 2026-11-23. JVM only. Every step had a timeout that kills the process. Gradle and the oracle batch ran under the shared build lock.

Since the evidence revisions, nothing changed in `scripts/oracle/{preferences,creation-defaults,menus,menu-windows,cli,integrations,project-options,visual-export}` or `fixtures/{preferences,creation-defaults}` (`git log 2653be42..434c72f2`). The Evolution side of the preferences oracle (`fixtures/preferences/evolution.tsv`, written by `scripts/oracle/preferences/run.sh` from the same JAR) was reused, not regenerated. Regenerating it from the `c5d14835` checkout would read a newer `WindowOptions.java`.

## Host tests

```sh
./gradlew :core:jvmJar :core:jvmTest --tests '*EvolutionDefaultTemplateTest' --tests '*SocProgramDesktopParityTest' \
  --tests '*Rv32DesktopOracleTest' --tests '*HeadlessCircuitCliTest' --tests '*SignalTraceTest' \
  :composeApp:testAndroidHostTest --tests '*MenuActionsParityExportTest' --tests '*EditorPreferencesEvolutionTest' \
  --tests '*PreferencesEvolutionDefaultsTest' --tests '*CreationDefaultsEvolutionTest' --tests '*CatalogWorkspaceBindingTest' \
  --tests '*EvolutionVisualExportOracleTest' --tests '*EvolutionHotkeysTest' --tests '*EditorKeyboardShortcutsTest' \
  --tests '*PropertyInspectorSessionTest' :composeApp:jvmTest --tests '*EvolutionVectorExportHostTest'
```

165 tests passed and 0 failed ([per class](logs/host-tests.tsv), [tasks](logs/gradle-tasks.txt)). `EditorPreferencesEvolutionTest` and `CreationDefaultsEvolutionTest` compare Mobile with the recorded `fixtures/preferences/mobile.tsv` and `fixtures/creation-defaults/mobile-*.circ`. After `creation-defaults/run.sh`, the tree had no changes, so Evolution still loads the Mobile files exactly as `evolution-load.tsv` records.

## Oracles

| Evidence | Command | Result |
| --- | --- | --- |
| `menus-jvm-434c72f2` | `scripts/oracle/menus/run.sh EVOLUTION_JAR OUT --no-build` | **1 new DIFF**. `edit-dup` now differs. The other 52 `SAME` and 6 accepted `DIFF` comparisons match the [`menus-c7b67eb5`](../menus-c7b67eb5/README.md) receipt. Board editor `SAME` (GECKO5, BASYS3). Verilog import `SAME` (hierarchy, gates, macros). See [compare.txt](menus/compare.txt) and [menu-actions.json](menus/menu-actions.json). |
| `preferences-jvm-434c72f2`, `preferences-defaults-jvm-434c72f2` | `./scripts/oracle/creation-defaults/run.sh EVOLUTION_JAR && python3 scripts/oracle/preferences/compare.py` | All 71 flagged preference and panel rows have the same result as before. Every `igual` row is `SAME`, and every `diferenca` row (GATE_SHAPE, TOOLBAR_PLACEMENT, FollowSystemTheme, POKE_WIRE_RADIX2, IntlOptions, WindowOptions, LayoutOptions) is a `DIFF` with its documented cause. See [comparison](preferences-comparison.tsv). |
| `cli-jvm-434c72f2` | `python3 -I scripts/oracle/cli/run.py --evolution-jar EVOLUTION_JAR --out OUT` | 80 cases, 64 PASS and 16 DIFF, all with recorded causes. Case-by-case, these results match `jvm-331-e6a238ad`. The only change is the `cli/version` text: Mobile now prints `Logisim Evolution Mobile 1.0.0`, after `57990364`. See [results.json](cli/results.json). |
| `integrations-jvm-434c72f2` | `python3 scripts/oracle/integrations/run.py EVOLUTION_JAR --out OUT` | All suites exit with 0. `fpga-boards` PASS (2518 fields), `vectors` PASS (14), `hdl-import` PASS (44 rows), `verilog-evolution-import` PASS, `hdl-declarations` 18/26 matching, 8 accepted only by Mobile, 0 differing, `blif-cosim` PASS (350 steps), `fpga-hdl` PASS, `soc-drivers` 3/5 (the accepted difference), `soc-program-desktop` SAME, `tcl-desktop-protocol` OK. These are the same results as `jvm-331-e6a238ad` and `hdl-declarations-c7b67eb5`. See [results.json](integrations/results.json). |
| `menu-windows-jvm-434c72f2` | `scripts/oracle/menu-windows/run.sh EVOLUTION_JAR` | Assembly window same=389, undefined-kept=96, differences=0. Print same=48, differences=0. FPGA Commander differences=0. The [log](logs/menu-windows.txt) is identical to `menu-windows-6ea6bf20` except for the core JAR hash. |
| `project-options-jvm-434c72f2` | `ORACLE_TIMEOUT=30 scripts/oracle/project-options/run.sh EVOLUTION_JAR OUT` | PASS, 21 project-option fixtures match Evolution ([run](project-options/run.txt)). |
| `visual-export-jvm-434c72f2` | `python3 -I scripts/oracle/visual-export/run.py --evolution-jar EVOLUTION_JAR --out OUT` | GIF and JPEG PASS on the historical Android and iOS rasters, as the script documents. WaveDrom has the same accepted `q:name` DIFF. The SVG check results are identical to `jvm-331-e6a238ad`, and only the Mobile SVG hash changed. See [run-results.json](visual-export/run-results.json). This evidence is used for `format/image-wavedrom`, whose Mobile side comes from `core-jvm.jar` at this revision. `format/image-jpg` stays flagged because its raster input is the historical device PNG, and `c3075994` changed the renderer that produces that PNG. A new device export (`visual-export34`) is needed. |
| `visual-export-vector-jvm-434c72f2` | `python3 -I scripts/oracle/visual-export/compare_exports.py --evolution-jar EVOLUTION_JAR --out OUT` | 13/13 PASS: SVG and TikZ for qualification, ANSI and IEC gates and labels, and the printer view. `results.json` is identical to `visual-export-printer-markers-74564f1a`. See [compare-results.json](visual-export/compare-results.json). |
| `corpus-replay-434c72f2` | `python3 scripts/oracle-replay-receipt.py --source-root . --mobile-revision 434c72f2… --desktop-revision d58a61e0… --core-jar core-jvm.jar --desktop-jar EVOLUTION_JAR --core-sha256 6f2ba0cf… --desktop-sha256 d0b287d1… --output OUT` | Passed. The 7 circuit fixtures produced 222 records with 0 differences. The 6 temporal fixtures produced 155 records in `settle` and `step`. The temporal TSVs are byte-identical to `oracle-replay-4764cf9a`. The archived `source/` and `inputs/` copies were not committed. Their hashes are in [receipt.json](oracle-replay/receipt.json). |

## New difference: `MenuEdit/dup` (`edit-dup`)

[compare.txt](menus/compare.txt) records a document difference:

```
DIFF edit-dup
-    <comp lib="1" loc="(410,310)" name="NOT Gate" />
+    <comp lib="1" loc="(420,310)" name="NOT Gate" />
```

Reproduction: at `434c72f2`, run `./gradlew :core:jvmJar :composeApp:testAndroidHostTest --tests '*MenuActionsParityExportTest'`, then `scripts/oracle/menus/run.sh EVOLUTION_JAR OUT --no-build`. `MenuActionsParityExportTest` selects the NOT gate at (400,300), calls `duplicateSelected()`, moves the preview to (410,310), and confirms. Evolution's Duplicate with the pointer outside the canvas (`copyComponents(comps, false)`) places the copy at (410,310), an offset of (10,10). Mobile now saves the copy at (420,310).

Cause: `3ac513e1` anchors the duplicate and paste preview at the centre of the fragment bounds (`pasteAnchor`), rounded to the grid. Before that commit, the anchor was the first component's location. For this NOT gate, the anchor is (390,300), so a tap at (410,310) gives an offset of (20,10). The harness still taps the point computed for the old anchor. It also records `edit-dup.offset` as the constant `10,10`, which is why `state:edit-dup.offset` still reads `SAME`. The row is `divergente` until someone decides whether to adapt the harness to the new anchor or to change the anchor.
