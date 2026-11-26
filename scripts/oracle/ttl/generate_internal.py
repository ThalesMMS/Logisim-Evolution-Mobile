#!/usr/bin/env python3
"""Generate TtlInternalStructureData.kt from TtlInternalStructureOracle display lists.

Input: the shaped.txt and rectangular.txt files written by TtlInternalStructureOracle. Lines that start with "~"
(state dots) are dropped. Text from Ttl74123.drawTiming ("^") becomes the {pulse1}/{pulse2} placeholders that the
mobile renderer fills from the Rext/Cext attributes.

Output encoding, one operation per line, in component coordinates (facing east, location 0,0):
  w N            stroke width for the following operations (initially 1)
  c AARRGGBB     colour for the following operations (initially ff000000, the component colour)
  r|R x y w h [radius]   stroked|filled rectangle
  o|O x y w h            stroked|filled ellipse
  l x y x y ...          stroked polyline
  p|P M x y L x y Q .. C .. Z   stroked|filled path
  t family size style cx baseline rotation text   text centred on cx, on the baseline

With --state (the TtlInternalStructureOracle --state output), the register value dots are generated too: one
"d x y w h" line per dot in painting order, followed by "t family size style cx baseline rotation" when the dot has a
value label. The Evolution values after every fixture row go to core/src/jvmTest/resources/ttl-internal-state.tsv
("model, row, dot values, 74123 timing label states"), which TtlInternalStateTest replays.
"""
import argparse
import re
from pathlib import Path


def parse(path: Path):
    models, header, current = {}, {}, None
    for raw in path.read_text(encoding='utf-8').splitlines():
        if not raw or raw.startswith('#'):
            continue
        if raw.startswith('= '):
            fields = raw.split(' ')
            current = fields[1]
            models[current] = []
            header[current] = dict(f.split('=', 1) for f in fields[2:])
            continue
        models[current].append(raw)
    return models, header


def encode(model: str, ops: list[str]) -> list[str]:
    out, width, color, pulse = [], '1', 'ff000000', 0
    for raw in ops:
        if raw.startswith('~'):
            continue
        attribute = raw.startswith('^')
        line = raw[1:] if attribute else raw
        kind = line[0]
        if kind == 'T':
            _, argb, family, size, style, cx, baseline, rotation, _advance, text = line.split(' ', 9)
            if attribute:
                pulse += 1
                text = '{pulse%d}' % pulse
            if argb != color:
                out.append('c ' + argb)
                color = argb
            out.append(' '.join(['t', family, size, style, cx, baseline, rotation, text]))
            continue
        fields = line.split(' ')
        mode, argb, stroke = fields[1], fields[2], fields[3]
        rest = fields[4:]
        if argb != color:
            out.append('c ' + argb)
            color = argb
        if mode == 's' and stroke != width:
            out.append('w ' + stroke)
            width = stroke
        fill = mode == 'f'
        if kind == 'R':
            if rest[-1] == '0':
                rest = rest[:-1]
            out.append(('R ' if fill else 'r ') + ' '.join(rest))
        elif kind == 'O':
            out.append(('O ' if fill else 'o ') + ' '.join(rest))
        elif kind == 'P':
            tokens = rest
            commands = tokens[0::3]
            simple = (not fill and tokens[0] == 'M' and len(tokens) % 3 == 0
                      and all(c == 'L' for c in commands[1:]))
            if simple:
                points = [t for i, t in enumerate(tokens) if i % 3]
                out.append('l ' + ' '.join(points))
            else:
                out.append(('P ' if fill else 'p ') + ' '.join(tokens))
        else:
            raise SystemExit(f'{model}: unknown operation {line!r}')
    if attribute_count(ops) != pulse:
        raise SystemExit(f'{model}: attribute text count mismatch')
    return out


def attribute_count(ops):
    return sum(1 for op in ops if op.startswith('^T'))


VALUE_COLORS = {'ff00d200': '1', 'ff006400': '0', 'ff2828ff': 'U', 'ffc00000': 'E'}


def parse_state(path: Path):
    templates, replay, model = {}, [], None
    for raw in path.read_text(encoding='utf-8').splitlines():
        if not raw or raw.startswith('#'):
            continue
        if raw.startswith('= '):
            model = raw[2:]
            templates[model] = []
        elif raw.startswith('template '):
            op = raw[len('template '):]
            if op.startswith('^'):
                continue
            fields = op[1:].split(' ')
            if op.startswith('~O f'):
                templates[model].append('d ' + ' '.join(fields[4:8]))
            elif op.startswith('~P f'):
                numbers = [float(t) for t in fields[4:] if t not in 'MLCQZ']
                xs, ys = numbers[0::2], numbers[1::2]
                n = lambda v: ('%.3f' % v).rstrip('0').rstrip('.')
                templates[model].append('d ' + ' '.join(n(v) for v in (min(xs), min(ys), max(xs) - min(xs), max(ys) - min(ys))))
            elif op.startswith('~T'):
                f = op[1:].split(' ', 9)
                templates[model].append(' '.join(['t', f[2], f[3], f[4], f[5], f[6], f[7]]))
            else:
                raise SystemExit(f'{model}: unexpected state operation {op!r}')
        elif raw.startswith('row '):
            row, _texts, fills, labels = raw[4:].split('\t')
            values = ''.join(VALUE_COLORS[c] for c in fills.split(',') if c)
            active = ''.join('1' if c == 'ff00d200' else '0' for c in labels.split(',') if c)
            replay.append(f'{model}\t{row}\t{values}\t{active}')
    return {m: t for m, t in templates.items() if t}, replay


def kotlin_string(model: str, lines: list[str]) -> str:
    body = '\n'.join(lines)
    if '"""' in body or '$' in body:
        raise SystemExit(f'{model}: cannot embed operations in a raw string')
    if len(body.encode('utf-8')) > 60000:
        raise SystemExit(f'{model}: display list too large for one string constant')
    return '"""\n' + body + '\n"""'


def base_entries(path: Path) -> tuple[str, dict[str, dict[str, str]]]:
    """Source line and map entries ("model" to ...) of a generated TtlInternalStructureData.kt."""
    text = path.read_text(encoding='utf-8')
    # A merged file names the source of its other models last.
    source = re.search(r' \* Source: (.*?)\. Do not edit by hand\.', text).group(1).split('; the other models: ')[-1]
    maps = {}
    for match in re.finditer(r'    val (\w+): Map<String, String> = mapOf\(\n(.*?)\n    \)', text, re.S):
        maps[match.group(1)] = {m.group(1): m.group(0) for m in re.finditer(
            r'^        "(\w+)" to (""".*?\n"""|"[^"\n]*"),$', match.group(2), re.S | re.M)}
    return source, maps


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('shaped', type=Path)
    parser.add_argument('rectangular', type=Path)
    parser.add_argument('--state', type=Path, help='TtlInternalStructureOracle --state output')
    parser.add_argument('--source', required=True, help='Evolution revision and JAR digest recorded in the header')
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument('--base', type=Path, help='Committed TtlInternalStructureData.kt whose other models are kept')
    parser.add_argument('--only', help='Space-separated models taken from these recordings; needs --base')
    args = parser.parse_args()
    if bool(args.base) != bool(args.only):
        raise SystemExit('--base and --only go together')
    only = set(args.only.split()) if args.only else None
    base_source, base = base_entries(args.base) if args.base else (None, {})

    def entry(name: str, model: str, generated: str | None) -> str | None:
        # A model outside --only keeps the committed entry (or its absence).
        return generated if only is None or model in only else base.get(name, {}).get(model)
    shaped, header = parse(args.shaped)
    rectangular, _ = parse(args.rectangular)
    if shaped.keys() != rectangular.keys():
        raise SystemExit('The variants record different models')
    out = [
        'package dev.logisim.mobile.core',
        '',
        '/**',
        ' * What the desktop TTL factories draw with ShowInternalStructure=true, recorded by',
        ' * scripts/oracle/ttl/TtlInternalStructureOracle.java and generated by scripts/oracle/ttl/generate_internal.py.',
        f' * Source: {args.source if only is None else args.source + " for " + ", ".join(sorted(only, key=lambda m: (len(m), m))) + "; the other models: " + base_source}. Do not edit by hand.',
        ' */',
        'internal object TtlInternalStructureData {',
        '    /** Declaring class of paintInternal for each model, as the inventory in docs/ttl-validation.md lists it. */',
        '    val painters: Map<String, String> = mapOf(',
    ]
    for model in shaped:
        out.append(f'        "{model}" to "{header[model]["paintInternal"]}",')
    out += ['    )', '', '    /** ANSI (Evolution "shaped") gate symbols; DIN 40700 uses them too. */', '    val shaped: Map<String, String> = mapOf(']
    encoded = {}
    for model, ops in shaped.items():
        encoded[model] = encode(model, ops)
        out.append(entry('shaped', model, f'        "{model}" to {kotlin_string(model, encoded[model])},'))
    out += ['    )', '', '    /** IEC (Evolution "rectangular") gate symbols, for the models whose drawing differs. */', '    val rectangular: Map<String, String> = mapOf(']
    differing = 0
    for model, ops in rectangular.items():
        lines = encode(model, ops)
        kept = entry('rectangular', model, f'        "{model}" to {kotlin_string(model, lines)},' if lines != encoded[model] else None)
        if kept:
            differing += 1
            out.append(kept)
    out += ['    )']
    if args.state:
        templates, replay = parse_state(args.state)
        out += ['', '    /** Register value dots, in Evolution painting order; TtlInternalStructure binds them to the model state. */',
                '    val stateDots: Map<String, String> = mapOf(']
        for model in shaped:
            kept = entry('stateDots', model, f'        "{model}" to {kotlin_string(model, templates[model])},' if model in templates else None)
            if kept:
                out.append(kept)
        out += ['    )']
        resource = args.root / 'core/src/jvmTest/resources/ttl-internal-state.tsv'
        resource.write_text('# model\trow\tdots\t74123 timing labels (1 = Value.TRUE colour)\n' + '\n'.join(replay) + '\n', encoding='utf-8', newline='\n')
        print(f'Wrote {len(templates)} dot templates and {len(replay)} replay rows')
    out += ['}', '']
    target = args.root / 'core/src/commonMain/kotlin/dev/logisim/mobile/core/TtlInternalStructureData.kt'
    target.write_text('\n'.join(out), encoding='utf-8', newline='\n')
    print(f'Generated {len(encoded)} models, {differing} with a separate IEC drawing, into {target}')


if __name__ == '__main__':
    main()
