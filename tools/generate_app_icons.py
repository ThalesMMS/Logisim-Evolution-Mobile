#!/usr/bin/env python3
"""Render the editor-inspired AND gate for Android and Apple launchers."""
from __future__ import annotations

import argparse
from itertools import product
import json
from math import ceil, floor, sqrt
from pathlib import Path
import struct
import sys
import zlib

ROOT = Path(__file__).resolve().parents[1]
ART_SIZE = 200
GRID_STEP = 10
GRID_RADIUS = 0.75
CENTER_Y = ART_SIZE / 2
GATE_ARC_X = 102
# Left edge, then the horizontal and vertical radii of the right half-ellipse.
GATE_OUTER = (65, 43, 46)
GATE_INNER = (72, 36, 39)
TERMINALS = ((28, 78), (28, 122), (172, 100))
TERMINAL_RADIUS = 8.75
WIRE_WIDTH = 6.25
# Wire ends overlap the outline without entering the transparent themed icon.
WIRES = ((28, 68, 78), (28, 68, 122), (142, 172, 100))

ROLES = ("canvas", "grid", "ink", "signal")
CANVAS, GRID, INK, SIGNAL = range(len(ROLES))
LIGHT = ("#FEFEFE", "#C7C7C7", "#202020", "#038303")
DARK = ("#282828", "#595959", "#F6F6F6", "#40D446")


def gate_path(shape: tuple[int, int, int]) -> str:
    left, radius_x, radius_y = shape
    top, bottom = CENTER_Y - radius_y, CENTER_Y + radius_y
    return f"M{left},{top:g} H{GATE_ARC_X} A{radius_x},{radius_y} 0 0,1 {GATE_ARC_X},{bottom:g} H{left} Z"


def wire_path() -> str:
    return " ".join(f"M{left},{y} H{right}" for left, right, y in WIRES)


def circle_path(x: float, y: float, radius: float) -> str:
    return (
        f"M{x - radius:g},{y:g} a{radius:g},{radius:g} 0 1,0 {2 * radius:g},0 "
        f"a{radius:g},{radius:g} 0 1,0 {-2 * radius:g},0 Z"
    )


def svg(palette: tuple[str, ...]) -> str:
    canvas, grid, ink, signal = palette
    terminals = "\n".join(
        f'  <circle cx="{x}" cy="{y}" r="{TERMINAL_RADIUS:g}" fill="{signal}"/>' for x, y in TERMINALS
    )
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {ART_SIZE} {ART_SIZE}" width="1024" height="1024">
  <defs>
    <pattern id="grid" width="{GRID_STEP}" height="{GRID_STEP}" patternUnits="userSpaceOnUse">
      <circle cx="{GRID_STEP / 2:g}" cy="{GRID_STEP / 2:g}" r="{GRID_RADIUS:g}" fill="{grid}"/>
    </pattern>
  </defs>
  <rect width="{ART_SIZE}" height="{ART_SIZE}" fill="{canvas}"/>
  <rect width="{ART_SIZE}" height="{ART_SIZE}" fill="url(#grid)"/>
  <path d="{wire_path()}" fill="none" stroke="{signal}" stroke-width="{WIRE_WIDTH:g}"/>
{terminals}
  <path d="{gate_path(GATE_OUTER)}" fill="{ink}"/>
  <path d="{gate_path(GATE_INNER)}" fill="{canvas}"/>
</svg>
'''


def sample_row(size: int, y: float) -> bytearray:
    """Rasterize one row of subpixel samples in drawing order."""
    row = bytearray(size)
    scale = size / ART_SIZE

    def paint(left: float, right: float, role: int) -> None:
        start = max(0, ceil(left * scale - 0.5))
        stop = min(size, floor(right * scale - 0.5) + 1)
        row[start:stop] = bytes([role]) * max(0, stop - start)

    grid_y = y % GRID_STEP - GRID_STEP / 2
    if abs(grid_y) < GRID_RADIUS:
        reach = sqrt(GRID_RADIUS ** 2 - grid_y ** 2)
        for x in range(GRID_STEP // 2, ART_SIZE, GRID_STEP):
            paint(x - reach, x + reach, GRID)
    for left, right, wire_y in WIRES:
        if abs(y - wire_y) < WIRE_WIDTH / 2:
            paint(left, right, SIGNAL)
    for x, terminal_y in TERMINALS:
        delta_y = y - terminal_y
        if abs(delta_y) < TERMINAL_RADIUS:
            reach = sqrt(TERMINAL_RADIUS ** 2 - delta_y ** 2)
            paint(x - reach, x + reach, SIGNAL)
    for (left, radius_x, radius_y), role in ((GATE_OUTER, INK), (GATE_INNER, CANVAS)):
        relative_y = (y - CENTER_Y) / radius_y
        if abs(relative_y) < 1:
            extent = radius_x * sqrt(1 - relative_y ** 2)
            paint(left, GATE_ARC_X + extent, role)
    return row


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))


def png(size: int, palette: tuple[str, ...]) -> bytes:
    colors = [bytes.fromhex(value[1:]) for value in palette]
    # Cache every four-sample blend for a 2× supersampled, opaque RGB PNG.
    blends = {
        samples: bytes(sum(colors[index][channel] for index in samples) // 4 for channel in range(3))
        for samples in product(range(len(ROLES)), repeat=4)
    }
    rows = bytearray()
    step = ART_SIZE / (size * 2)
    for y in range(size):
        top = sample_row(size * 2, (2 * y + 0.5) * step)
        bottom = sample_row(size * 2, (2 * y + 1.5) * step)
        rows.append(0)
        for x in range(0, size * 2, 2):
            rows.extend(blends[top[x], top[x + 1], bottom[x], bottom[x + 1]])
    return (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0))
        + png_chunk(b"IDAT", zlib.compress(rows, 9))
        + png_chunk(b"IEND", b"")
    )


def vector(paths: str, *, viewport: int = 108) -> str:
    return f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">
{paths}</vector>
'''


def adaptive_background() -> str:
    # The central 200 units are the visible 72 dp. Continue the grid into the
    # extra 18 dp on all sides so the background also covers launcher motion.
    bleed_size = ART_SIZE * 3 // 2
    centers = range(GRID_STEP // 2, bleed_size, GRID_STEP)
    # AAPT stores each path in a single string, so emit the dots one row at a time.
    dot_rows = "\n".join(
        f'    <path android:pathData="{" ".join(circle_path(x, y, GRID_RADIUS) for x in centers)}"'
        ' android:fillColor="@color/app_icon_grid"/>'
        for y in centers
    )
    return vector(f'''    <path android:pathData="M0,0 H{bleed_size} V{bleed_size} H0 Z" android:fillColor="@color/app_icon_canvas"/>
{dot_rows}
''', viewport=bleed_size)


def adaptive_foreground(monochrome: bool) -> str:
    signal = "#FFFFFF" if monochrome else "@color/app_icon_signal"
    outer, inner = gate_path(GATE_OUTER), gate_path(GATE_INNER)
    terminals = " ".join(circle_path(x, y, TERMINAL_RADIUS) for x, y in TERMINALS)
    if monochrome:
        gate = f'<path android:pathData="{outer} {inner}" android:fillType="evenOdd" android:fillColor="#FFFFFF"/>'
    else:
        gate = f'''<path android:pathData="{outer}" android:fillColor="@color/app_icon_ink"/>
        <path android:pathData="{inner}" android:fillColor="@color/app_icon_canvas"/>'''
    return vector(f'''    <group android:translateX="18" android:translateY="18" android:scaleX="0.36" android:scaleY="0.36">
        <path android:pathData="{wire_path()}" android:strokeWidth="{WIRE_WIDTH:g}" android:strokeColor="{signal}"/>
        <path android:pathData="{terminals}" android:fillColor="{signal}"/>
        {gate}
    </group>
''')


def colors_xml(palette: tuple[str, ...]) -> str:
    colors = "\n".join(f'    <color name="app_icon_{role}">{color}</color>' for role, color in zip(ROLES, palette))
    return f'<?xml version="1.0" encoding="utf-8"?>\n<resources>\n{colors}\n</resources>\n'


def adaptive_icon(monochrome: bool) -> str:
    tint_layer = '    <monochrome android:drawable="@drawable/app_icon_monochrome"/>\n' if monochrome else ""
    return f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/app_icon_background"/>
    <foreground android:drawable="@drawable/app_icon_foreground"/>
{tint_layer}</adaptive-icon>
'''


def generated_files() -> dict[str, bytes]:
    android = "androidApp/src/main/res"
    ios = "iosApp/Assets.xcassets"
    catalog_info = {"author": "xcode", "version": 1}
    ios_icon = {"idiom": "universal", "platform": "ios", "size": "1024x1024"}
    ios_icons = [
        {"filename": "AppIcon.png", **ios_icon},
        {
            "appearances": [{"appearance": "luminosity", "value": "dark"}],
            "filename": "AppIconDark.png",
            **ios_icon,
        },
    ]
    files = {
        "artwork/app-icon.svg": svg(LIGHT).encode(),
        "artwork/app-icon-dark.svg": svg(DARK).encode(),
        f"{android}/drawable/app_icon_background.xml": adaptive_background().encode(),
        f"{android}/drawable/app_icon_foreground.xml": adaptive_foreground(False).encode(),
        f"{android}/drawable/app_icon_monochrome.xml": adaptive_foreground(True).encode(),
        f"{android}/values/app_icon_colors.xml": colors_xml(LIGHT).encode(),
        f"{android}/values-night/app_icon_colors.xml": colors_xml(DARK).encode(),
        f"{android}/mipmap-anydpi-v26/ic_launcher.xml": adaptive_icon(False).encode(),
        f"{android}/mipmap-anydpi-v33/ic_launcher.xml": adaptive_icon(True).encode(),
        f"{ios}/Contents.json": b'{"info":{"author":"xcode","version":1}}\n',
        f"{ios}/AppIcon.appiconset/Contents.json": (json.dumps({"images": ios_icons, "info": catalog_info}, indent=2) + "\n").encode(),
        f"{ios}/AppIcon.appiconset/AppIcon.png": png(1024, LIGHT),
        f"{ios}/AppIcon.appiconset/AppIconDark.png": png(1024, DARK),
    }
    for bucket, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        files[f"{android}/mipmap-{bucket}/ic_launcher.png"] = png(size, LIGHT)
    return files


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    options = parser.parse_args()
    stale = []
    for relative, contents in generated_files().items():
        path = ROOT / relative
        if options.check:
            if not path.is_file() or path.read_bytes() != contents:
                stale.append(relative)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(contents)
    if stale:
        print("Generated application icons are stale: " + ", ".join(stale), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
