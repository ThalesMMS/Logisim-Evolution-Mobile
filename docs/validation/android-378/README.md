# Android verification for #378

Android pass for the items #368 and #377 left open, for the four matrix rows that needed a recheck, and for the changes of 2026-11-23 (#374, #375, #376). Run on 2026-11-23. Emulators only; no physical device was used.

## Environment

| Item | Value |
| --- | --- |
| Host | Windows 11 x64, JDK 21.0.12, Gradle 9.6.1, Python 3.13, Android SDK emulator 35 image `google_apis/x86_64` |
| Product revision | `main` at `ae3d5422838dc9ccbc30fcdb08bc1402fac47964`, app 1.0.0 build 7 (`app.properties`), e2e build type |
| Fix revision (#379) | `560091d37adfb5373100af26e821150e1a744d39` (`ae3d5422` plus the focus fix in `EditorKeyboardShortcuts.kt` and its host test) |
| Phone AVD | `logisim35`: Pixel 7, 1080×2400, 420 dpi, API 35, `hw.keyboard=no`, `-gpu host`, English system locale. Fingerprint `google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys` |
| Keyboard AVD | `logisim35kb`: Pixel Tablet, 2560×1600, 320 dpi, API 35, `hw.keyboard=yes`, same image |
| APKs at `ae3d5422` | app `54792ef49faefdb505cb4b5172eb61013ec35c6b537334b7c14af123383bfc86`, androidTest `d32b2f3212e815a071582e517ddd26fad282178af987853f291614986f9da93a`, snapshot `cf6c640ffe14ceffc0cab7db7f5c0d8807fc0a3aced00a0ee3e3b1af6b2667fc` ([build-receipt.json](build-receipt.json)) |
| App APK at `560091d3` | `8105016fa6eb7ec1a96e3d7d5425922a2ba13d402b33eeb9790a079a42e14617` (androidTest and snapshot APKs unchanged) |

Build: `./gradlew -PandroidTestBuildType=e2e :androidApp:assembleE2e :androidApp:assembleE2eAndroidTest :androidSnapshot:assembleDebug`.

The Windows checkout must use LF endings. With Git's default `core.autocrlf=true`, `fixtures/visual-export/qualification.circ` fails its digest check and bundled text resources change. The checkout used `core.autocrlf=false` and `core.eol=lf`. The Python harnesses ran with `PYTHONUTF8=1`, `ANDROID_HOME` set and `--aapt2 <sdk>/build-tools/36.0.0/aapt2.exe`. Windows hosts are outside the project's scope, and no host-specific change was made to the scripts.

## Results

| # | Item | Result | Evidence |
| --- | --- | --- | --- |
| 1 | #368 Insert a subcircuit from the Circuits tab | PASS, steps 1–6 on the phone. Step 7: TalkBack exposes the label and the reason (below). | [issue368/](issue368) |
| 2 | `MenuFile/exportImage`, `format/image-png` (`visual-export34`) | PASS | [e2e/visual-export34](e2e/visual-export34) |
| 2 | `format/image-jpg` (fresh export against Evolution's JPEG) | JPEG 13/13 checks PASS, GIF 20/20 | [jpeg-compare/](jpeg-compare) |
| 2 | `behavior/attribute-table` (`inspector-invalid-value-focus-and-undo`) | PASS | [e2e/inspector](e2e/inspector) |
| 3 | Editing: `editing-create-properties`, `editing-wires-truth-table`, `editing-selection-history`, `editing-viewport-touch-parity` | 4/4 PASS | [e2e/editing](e2e/editing) |
| 3 | SAP-1: `sequential-simulation-and-recovery` | PASS (56/56 manual half cycles) | [e2e/sequential](e2e/sequential) |
| 3 | Remaining colors (`android_display_preferences_proof.py --only 311`) | PASS after three harness fixes (below) | [remaining-colors-311/](remaining-colors-311) |
| 3 | Shortcuts, injected with `adb shell input keyboard` | All steps PASS on `560091d3`. Found #379 on `ae3d5422` | [hotkeys/](hotkeys) |
| 4 | #375/#376 Spanish | PASS | [issue375-376/](issue375-376) |
| 4 | #374 7400 and 7474 with Show internal structure | PASS | [issue374/](issue374) |

### 1. #368 (phone, `ae3d5422`)

`examples/hierarchy.circ` was pushed to Downloads and imported with **File → Import a copy** through DocumentsUI (`File hierarchy.circ opened with 4 compatibility adjustments.`).

1. In **Circuits**, `circuit-insert-Hierarchy` is disabled and `circuit-insert-Inversor` is enabled. Tapping the Inversor row opens its definition (header `Inversor`, `Circuit Inversor.`). [steps-1-2.json](issue368/steps-1-2.json), [02-circuits-tab.png](issue368/02-circuits-tab.png)
2. **Insert** on Inversor closes the sheet with `Tap the grid to insert Inversor into Hierarchy.`, and the header stays on Hierarchy. A tap adds a second instance with the same square symbol, one input and one output, and the status reads `Subcircuit inserted. Edit mode.` [06-placed.png](issue368/06-placed.png)
3. Undo removes it and Redo restores it (`Undo, Add Subcircuit`). Close the tab with **Keep changes**, force-stop, relaunch, then **Projects… → hierarchy**: both instances are there. [step-3a.json](issue368/step-3a.json), [step-3b.json](issue368/step-3b.json), [09-reopened.png](issue368/09-reopened.png)
4. **⋮ → Insert into Hierarchy** on Inversor works the same way. On Hierarchy the item is disabled with `Hierarchy is the circuit in view. A circuit cannot contain itself.` [steps-4-5.json](issue368/steps-4-5.json), [03-menu-self.png](issue368/03-menu-self.png)
5. Cycle: with Inversor open, Hierarchy's Insert is disabled and its menu item reads `Hierarchy contains Inversor. Inserting it would create a recursive circuit.` [13-cycle-menu.png](issue368/13-cycle-menu.png)
6. **Add circuit…** `Chip`, an Input placed in it, back to Hierarchy, **Insert** (`circuit-insert-circuit-1`) and a tap: `Subcircuit inserted. Edit mode.` [step-6.json](issue368/step-6.json), [15-chip-placed.png](issue368/15-chip-placed.png)
7. TalkBack 14 running (`enabled_accessibility_services` = TalkBackService), with the tree read by the `androidSnapshot` instrumentation using `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`. Each Insert button carries `Insert {name} into Hierarchy`. The disabled one has the state description `Hierarchy is the circuit in view. A circuit cannot contain itself.`. Each target is 51×48 dp. [step-7-talkback.json](issue368/step-7-talkback.json). The spoken output itself was not recorded. On this API 35 image, an injected tap under TalkBack activated the item instead of only moving the focus. Two attempts inserted an extra instance, which was undone, so announcements were not driven by touch exploration.

### 2. Rows that needed a recheck

- `visual-export34` exported PNG `0e620672…5b50` (1508×908) and PDF `a3891e78…1499`, shared to the test receiver and opened print preview. The project XML was unchanged.
- `format/image-jpg`: the new PNG was compared with Evolution's exports from the same fixture. The Evolution side is `docs/parity-evidence/jvm-331-e6a238ad/visual-export/evolution/qualification/visual_export_qualification.{jpg,gif}`, reused because Evolution `d58a61e0` and the fixture did not change. The tool was `RasterCompare` with `core-jvm.jar` `6f2ba0cf…b6b9` built at `ae3d5422` and kotlin-stdlib 2.4.21. JPEG: 13 PASS, 0 FAIL. GIF round trip: 20 PASS, 0 FAIL. The 1-pixel frame and the 78 regions match as before. The byte difference from the system JPEG encoder stays documented in the row.

### 3. Android scenarios not rerun by #377

The e2e runner commands were `python3 scripts/e2e-android.py --serial emulator-5560 --language en --scenario {inspector,visual-export34,editing,sequential}` (`visual-export34` also takes `--build-receipt build-receipt.json`). Each [manifest](e2e) records the APK hashes, an unchanged source tree and the device.

The remaining-colors proof needed three fixes to `scripts/android_display_preferences_proof.py`, all in the harness. The File menu now lists the examples above **Preferences…**, so on a phone the item sits below the fold and the menu scrolls to it. The color row's merged description is read from a descendant at the same bounds. The color picker's hex field is scrolled into view, and the soft keyboard is hidden before **Apply**. Before each attempt the app was reset with `pm clear`. Attempts 1–3 stopped at those harness steps, and attempt 4 passed.

Shortcuts ran on the keyboard AVD from `560091d3` after `pm clear`, with `show_ime_with_hard_keyboard=0` ([results.json](hotkeys/results.json)):

- Ctrl+1…0 follow the default.templ toolbar: Poke, Edit, Wiring, Text, Image, Input, Output, NOT, AND, OR.
- Ctrl+9, W, =, R and a tap place an AND facing South, Wide, with 3 inputs. M and − on the selection change it to Medium with 2 inputs and leave the zoom at 177%. Insert pastes at the tapped destination. − with nothing selected zooms from 177% to 142%.
- Shift, Shift opens Find Action. Ctrl+Shift+E and Ctrl+P open the Export drawing panel. Ctrl+Shift+U/D reorder `hk` (`Circuit order updated.`).
- Preferences: tools 11–14 start unassigned (`—`). Ctrl+S reports `Already assigned to Save.`, and Ctrl+K reports `Already assigned to Automatic ticks.`. Ctrl+J, Ctrl+G, Ctrl+B and Ctrl+H select XOR, NAND, NOR and D flip-flop. With double Shift off, Shift, Shift does nothing. Both settings hold after a force-stop and relaunch.
- Restore shortcut defaults disables itself and shows the four tools as `—` again. Ctrl+J becomes inert. With double Shift switched back on and the canvas focused, Shift, Shift opens Find Action.
- LED tool: H, then L sets Auto Label `l1`; V; T. Three LEDs `l1`, `l2`, `l3` get Show label No, Yes, No. Ctrl+Shift+W opens the project list with Close session.

JVM at `560091d3`: `./gradlew :composeApp:testAndroidHostTest --tests '*EvolutionHotkeysTest' --tests '*EditorKeyboardShortcutsTest'` 11 and 8 tests, 0 failures. The hotkey preference rows were checked with `./gradlew :composeApp:testAndroidHostTest --tests '*EditorPreferencesEvolutionTest'` and `python3 scripts/oracle/preferences/compare.py` against the committed `fixtures/preferences/evolution.tsv` (Evolution `d58a61e0`). The result is identical to the 434c72f2 comparison: 128 SAME and 45 DIFF overall, and the hotkey rows differ only in the documented Redo aliases ([comparison](preferences/preferences-comparison-560091d3.tsv)).

### 4. Changes from 2026-11-23

- #375/#376 (app language set to Español in Preferences): the menus read Archivo, Editar, Ver, Proyecto, Simular, FPGA and Ayuda. **Ver** lists Acercar and Alejar. The two-pin LED inspector shows `Color con polarización inversa`, and POR shows `Tamaño de dibujo`. The DMA SoC shows `Bus conectado`, `Bus de origen` and `Bus de destino`, and the SoC Bus has no "Autobús". [inspector.json](issue375-376/inspector.json), [menus.json](issue375-376/menus.json)
- #374: with Show internal structure on, the 7400 shows four NAND gates wired to the pins with Vcc and GND labels. The 7474 shows two D flip-flops with green `0` value dots. In the dark theme the strokes are white. After Rotate Left on the 7400 and Rotate Right on the 7474, each structure turns with its package and stays inside it. The 7474's `Q` touches its output wire as in Evolution's raster (`docs/parity-evidence/ttl-internal-structure-bd0e4e77/composites/state/7474.png`). [issue374/](issue374)

## Failure found: #379

At `ae3d5422`, the shortcuts stopped working on the keyboard AVD after these steps: in the sidebar **Search** tab, type `Preferences`, open the result and close it with Back. No node was focused, and Ctrl+6 did nothing until the canvas was tapped. The path through Find Action did not lose focus. The fix (`560091d3`) makes the shortcut target take focus back when the window is focused and nothing in the editor holds it. `EditorShortcutFocusHostTest` fails without the fix (`expected [TOOL_SELECT_2, TOOL_SELECT_3] but was [TOOL_SELECT_2]`) and passes with it.

The original device conditions could not be recreated after the app-data reset. The on-screen keyboard now appeared in the Search field despite `show_ime_with_hard_keyboard=0`, and with it focus returned to the field. Under those conditions the `ae3d5422` and `560091d3` APKs both pass the same steps ([issue379/](issue379)). So the device run at `560091d3` confirms there is no regression, and the host test is the discriminating check.

## Matrix

New evidence: `export34-android-ae3d5422`, `visual-export-raster-android-ae3d5422`, `inspector-android-ae3d5422`, `editing-android-ae3d5422`, `sap1-android-ae3d5422`, `remaining-colors-android-ae3d5422`, `ttl-internal-android-ae3d5422`, `hotkeys-android-560091d3`, `hotkeys-jvm-560091d3`, `hotkey-preferences-jvm-560091d3`.

Two iOS runs could not be repeated here and were removed from rows whose watched files changed after them. In each case the row says so and final acceptance (#243) covers iOS:

- `export34-ios-2653be42`: the iOS 27.2 Files save sheet blocks the harness.
- `hotkeys-ios-2653be42`: it predates #379.

## Not covered

- TalkBack's spoken output (step 7 records the semantics TalkBack reads).
- Physical devices and keyboards (out of scope).
- iOS.
