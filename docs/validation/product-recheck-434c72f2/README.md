# Product rows rechecked with native and host scenarios at `434c72f2` (#377)

The app was built from `434c72f2f27d3708635f9e6db9ac21f13e74981b` (`main`) in a clean worktree, with no code changes before the runs. Run on 2026-11-23 on macOS 27.2.1 arm64 with Xcode 27.2 (27B52) and Homebrew OpenJDK 21.0.13. Coverage is limited to the simulator and the JVM host. Android was not available on this host. The JVM oracles for the same revision are in [`docs/parity-evidence/product-recheck-434c72f2`](../../parity-evidence/product-recheck-434c72f2/README.md).

## iOS simulator

- Device: iPhone 17 Pro simulator `E403BD9B-650C-4DE3-9B81-E63A80C72C87`, iOS 27.2 runtime (`com.apple.CoreSimulator.SimRuntime.iOS-27-2`).
- App: `xcodegen generate --spec iosApp/project.yml && xcodebuild -project iosApp/LogisimMobile.xcodeproj -scheme LogisimMobile -configuration Debug -destination 'platform=iOS Simulator,id=<udid>' -derivedDataPath iosApp/DerivedData CODE_SIGNING_ALLOWED=NO build`, run under the shared build lock. The bundle tree SHA-256 is `516099a305642684f54c1b02ba06c05bc548f5913502e85b8ed8e535692492eb`, and the executable SHA-256 is `300cf7cd2ac626426d18f93fb1c37eec94102d92537d092ab3f5a77f08d8184a`.
- Harness: `python3 scripts/run-ios-e2e.py --simulator <udid> --baseline-sha 434c72f2… --app iosApp/Build/Products/Debug-iphonesimulator/LogisimMobile.app --output OUT --build-lock /tmp/logisim-build.lock --only-testing …`

| Run | Test | Result |
| --- | --- | --- |
| 1 | `DisplayPreferencesEndToEndTests/testRemainingColorsAndValueCharacters` | **PASS** (242 s). See `ios/run1-testRemainingColorsAndValueCharacters-*`. |
| 1 | `EditorEndToEndTests/testSap1AutomaticRunPause` | **PASS** (31 s). It also passed again in run 2 (28 s). |
| 1 | `EditorEndToEndTests/testSap1SequentialExecution` | **PASS** (161 s) |
| 1 | `EditorEndToEndTests/testEditingConnectionsHistoryAndViewport` | FAIL at contract step 49 (`assertDocument` after Edit > Delete): "No reachable control for Import or export circuit and project in editor-project-toolbar-menu". The final screen shows the deletion applied ("Selection removed.", empty canvas). The harness looked for the File item inside the Edit menu while that menu was still closing after `menu-delete`. |
| 2 | same test, `--test-timeout 1200` | Steps 1–64 passed, including step 49 and every clipboard, delete, undo, and redo checkpoint. At step 65 (`fitCircuit`), the test exceeded XCTest's 20-minute execution time allowance. |
| 3 | same test, `--test-timeout 2400` | **PASS** (1294 s). All 66 contract steps passed, including every `assertDocument` checkpoint for clipboard, delete, undo, and redo, and the viewport steps (`ios/run3-…`). The failures in runs 1 and 2 came from the harness and the time allowance. The app did not fail. |
| 1 | `VisualExportEndToEndTests/testEditorExportsPixelsAndCancelsNativeDelivery` | Not run to completion (missing prerequisite). The Files save sheet opened at "On My iPhone", but the harness did not see the "Logisim Evolution Mobile" directory heading. This matches the known iOS 27.2 Files-picker limitation recorded in `gate-2653be42` (see `ios/run1-…-final-screen.png`). |
| 1 | `InspectorEndToEndTests/testInvalidWidthPreservesProjectAndValidWidthIsOneUndoAction` | Not run to completion (missing prerequisite). The Files import route had the same 27.2 picker limitation. |
| 1, 2 | `InspectorEndToEndTests/testLabelLocationIsChosenFromAMenu` | Not run to completion (precondition). The test edits the first input pin of the open project (`A` or `Input…`). In isolation, the open project was SAP-1, which has no such pin (see `ios/run1-…-final-screen.png`). |

## JVM host

In the same Gradle run as the oracles ([per class](../../parity-evidence/product-recheck-434c72f2/logs/host-tests.tsv)):

- `hotkeys-jvm-434c72f2`: `EvolutionHotkeysTest` (11) and `EditorKeyboardShortcutsTest` (8) passed. These tests cover the Evolution `AppPreferences` hotkey defaults, each chord resolving to its command, tool numbers 1–14 skipping separators, Shift-Shift Find Action, the auto-label keys (T, V, H, A), add-tool rotation, and gate size and input keys. They also cover keys that fall through to zoom. The tests run the shared `EditorKeyboardShortcuts`/`EditorState` code that the flagged rows watch. They do not deliver hardware key events. The Android AVD and iPad hardware-keyboard scenarios (`hotkeys-android-2653be42` and `hotkeys-ios-2653be42`) were not rerun. The iPad harness `scripts/run-ios-fpga-tick.py` requires an iPad simulator, and this run was restricted to one iPhone.
- `product-jvm-434c72f2`: `CatalogWorkspaceBindingTest` (39) passed, covering continuous workspace saving and drafts.
- `PropertyInspectorSessionTest` (32) passed. It covers typed values, range rejection, multi-selection, and undo at the session level. It is recorded here, but it is not used as row evidence. Without the native invalid-value flow, `behavior/attribute-table` stays flagged.

## Rows

- **Cleared by run 1 of `remaining-colors-ios-434c72f2`**: the 11 `remaining-colors` rows. These are UNKNOWN_CHAR, STROKE_COLOR, the four WIDTH_ERROR colors, KMAP_CELL_TEXT_COLOR, TABLE_CURSOR_COLOR, TABLE_SELECTION_COLOR, KMAP1_COLOR, and KMAP2_COLOR. The Android scenario `remaining-colors-android-2653be42` was not rerun.
- **Cleared by `sap1-ios-434c72f2`**: `MenuSimulate/runToggle`. It previously had only Android evidence (`sap1-android-2653be42`).
- **Cleared by `hotkeys-jvm-434c72f2`**: the 23 hotkey rows (JVM host only, see above).
- **Cleared by `product-jvm-434c72f2`**: `preference/AUTOSAVE_ENABLED`, `preference/AUTOSAVE_INTERVAL`, `preference-panel/AutosaveOptions`, and `format/autosave`.
- **Cleared by run 3 of `editor-ios-434c72f2`**: `MenuEdit/undo`, `MenuEdit/redo`, `MenuEdit/copy`, `MenuEdit/paste`, `MenuEdit/delete`, and `MenuEdit/selall`. These rows previously had `editing-android-2653be42` evidence, and undo and redo also had `editor-ios-2653be42`. The Android scenario was not rerun.
- **Still flagged**: `MenuFile/exportImage`, `format/image-png`, and `format/image-jpg` (export34; iOS 27.2 Files picker; needs the Android `visual-export34` scenario or an iOS 27.0 runtime). `behavior/attribute-table` (Android `inspector-invalid-value-focus-and-undo`; the iOS equivalents were blocked by the Files picker and the open-project precondition).
