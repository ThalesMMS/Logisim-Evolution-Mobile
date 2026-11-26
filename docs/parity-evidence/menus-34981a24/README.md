# Menu actions compared with Evolution after the duplicate harness fix (#377)

- Mobile: `34981a24` (worktree branch on top of `main` `f83851b3`), clean tree. `core-jvm.jar` SHA-256 `6f2ba0cffdf59c73f686ea1970a7e9d53ee437e723f753444e382a931fd2b6b9`, the same as at `434c72f2` because `core/` did not change.
- Evolution: `d58a61e0c290361722b43df5b1a6b86c1f7c7f0d`, `logisim-evolution-5.1.0dev-all.jar` SHA-256 `d0b287d1695bc12f276f42b8db39af9a00e266ce0e0b53472b5aeab795a7a038`.
- Environment: macOS 27.2.1 arm64, Homebrew OpenJDK 21.0.13, Android host tests (JVM). Run on 2026-11-23 under the shared build lock, with a 1500 s timeout for Gradle and 900 s for the oracle.

## Command

```sh
./gradlew :core:jvmJar :composeApp:testAndroidHostTest --tests '*MenuActionsParityExportTest' --rerun
scripts/oracle/menus/run.sh EVOLUTION_JAR OUT --no-build
```

`MenuActionsParityExportTest` ran 3 tests with 0 failures ([gradle.txt](logs/gradle.txt)).

## Result

53 `SAME` and 6 accepted `DIFF` comparisons. Board editor `SAME` (GECKO5, BASYS3). Verilog import `SAME` (hierarchy, gates, macros). See [compare.txt](logs/compare.txt) and [menu-actions.json](menu-actions.json). The only change from [`product-recheck-434c72f2`](../product-recheck-434c72f2/README.md) is `edit-dup`, which went from `DIFF` to `SAME`. Both sides now save the copy of the NOT gate at (410,310), and `state:edit-dup.offset` reads `10,10` on both sides.

## `MenuEdit/dup` decision

The difference at `434c72f2` came from the harness, not the product. In Evolution `d58a61e0`, `LayoutEditHandler.duplicate()` runs `SelectionActions.duplicate`, then `SelectionBase.duplicateHelper` and `pasteHelper`:

- With the pointer on the canvas, `pasteHelper` centres the selection bounds on the pointer, rounded to 10 units (`centerX = bds.getX() + bds.getWidth() / 2`, `dx = round((mouse - centerX) / 10) * 10`).
- With the pointer off the canvas, `copyComponents(comps, false)` tries offsets that spiral out from (0,0). The first offset with no conflict for a single gate is (10,10).

Since `3ac513e1`, Mobile's duplicate preview follows the touch point with the centre of the selection bounds (`pasteAnchor`), and the user then places the copy. This matches Evolution's on-canvas placement. Before this run, `MenuActionsParityExportTest` still tapped (410,310), the point for the old first-component anchor, and recorded the offset as the constant `10,10`. At `34981a24`, the harness taps `pendingPaste.anchor + (10,10)`, which is where Evolution's off-canvas Duplicate puts the selection centre. It also records the offset measured from the placed copy. The product code did not change.

## `Base/WiringTool` and `Base/EditTool` (reused evidence, no new run)

`86cb33ec` flagged these rows when it changed `CircuitEditor.kt`. The change gives placed and pasted components their definition's `placedAttributes`. The rows now point to `editor-ios-434c72f2`, which was run 3 of `EditorEndToEndTests/testEditingConnectionsHistoryAndViewport` at `434c72f2`, where all 66 contract steps passed ([record](../../validation/product-recheck-434c72f2/README.md)). The scenario follows the shared contract `fixtures/user-scenarios/editor-actions-v1.json` (`editing-connections-history-viewport`), the same one the Android `editing-wires-truth-table` and `editing-selection-history` scenarios run:

- Wiring: `connectPorts` input→NOT and NOT→output, the `wired` document checkpoint, output values after toggling the input, `removeWire` with undo and redo, and a document checkpoint after each step.
- Edit tool: insertion and facing, `selectAll`, `selectComponent`, copy, paste (cancel and confirm, with distinct copies), delete, and undo and redo at each checkpoint, plus fit selection.

The old row evidence (`editing-android-2653be42`, and `editor-ios-2653be42` for the Wiring Tool) predates `86cb33ec`. It was dropped, as with the `MenuEdit` rows, because Android was not available. Neither scenario drags wires or components on the canvas. Both make connections through the accessible explorer, as the old evidence did.
