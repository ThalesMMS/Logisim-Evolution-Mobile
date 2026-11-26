# Android recheck of the TTL internal structure after #381 and #382

Native scenario for `behavior/ttl-internal-structure` after Mobile followed the 74266 and 7476 pinouts and the 15 diagrams Evolution redrew up to `21f1e158`. It replaces the #378 run (`ttl-internal-android-ae3d5422`, 7400 and 7474) on that row. Run on 2026-11-25 on an emulator; no physical device was used.

| Item | Value |
| --- | --- |
| Product revision | `main` at `e291da496f20b7496af80833e04f8d408d4f809e`, e2e build type |
| App APK | SHA-256 `a6dc2b57885c13d060174d43f050d0ebad9aaf51eba68d3f7cd054a69860db8c` ([build-receipt.json](build-receipt.json)) |
| AVD | `logisim35`: Pixel 7, 1080×2400, 420 dpi, API 35 `google_apis/x86_64`, English system locale |
| Host | Windows 11 x64, JDK 21.0.12.1; LF checkout (`core.autocrlf=false`, `core.eol=lf`) |

## Steps

1. Clear the app data, launch it and choose the light theme.
2. Project > Add circuit… creates `ttl382`; set the zoom to 100%.
3. For each of 7476, 74161, 74266, 7400 and 7474: Library, filter by the part number, Insert, Tool attributes > Show internal structure > Yes, tap the canvas.
4. Zoom > Fit (151%) and capture ([02-light-structure.png](02-light-structure.png)).
5. File > Preferences… > Theme > Dark, back to the editor and capture ([03-dark-structure.png](03-dark-structure.png)).
6. Edit tool, select the 74266, Rotate Left, deselect and capture ([04-dark-74266-rotated-left.png](04-dark-74266-rotated-left.png)).

[01-74266-placed-light.png](01-74266-placed-light.png) is the first 74266, placed in the example circuit before step 2.

## Result: PASS

- 7476: the `21f1e158` drawing, two J-K blocks with GND at the top and Vcc at the bottom (pins 13 and 5), and no value dots. It matches `docs/parity-evidence/ttl-evolution-21f1e158/internal/shaped/composites/7476.png`.
- 74161: the COUNTER block with its carry gate and `&`, no value dots.
- 74266: four XNOR gates with open-collector marks; the second gate drives pin 4 from pins 5 and 6 and the third drives pin 10 from 8 and 9, as in the SN74HC266 datasheet.
- 7400 and 7474: unchanged from the pinned drawing; the 7474 keeps its green `0` value dots.
- Dark theme: every stroke and label turns white and the 7474 dots keep their value colour.
- Rotate Left on the 74266 turns the diagram, labels included, with the package.

Rotate Right on the 7476 was refused with "Group editing cannot preserve the connections." because the rotated 74266 now overlapped its pins; the rotation path is shared by all TTL models and is covered by the 74266 step and by `TtlInternalStructureHostTest` (four facings).
