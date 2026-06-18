"""Post-process KanjiVG SVGs to make stroke numbers more visible and to color each
stroke along a rainbow gradient so users can see the writing direction at a glance.

- Stroke numbers: gray small → bold red, ~30% larger.
- Stroke colors: HSL-evenly-spaced palette by stroke index (1st=red, last=violet).
  The per-stroke colors override the default `stroke:#000000` declared on the parent
  group, giving an obvious order without needing animation.

Run after `download_kanjivg.py`. Idempotent (skips files that already contain the
marker comment we add).
"""

from __future__ import annotations

import colorsys
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SVG_DIR = ROOT / "app" / "src" / "main" / "assets" / "kanjivg"
MARKER = "<!-- enhanced -->"

PATH_RE = re.compile(
    r'(<path\s+id="kvg:[^"]+-s(\d+)"[^>]*?)(/>|>)',
    re.IGNORECASE,
)
STROKE_PATHS_GROUP_RE = re.compile(
    r'(<g\s+id="kvg:StrokePaths_[^"]+"\s+style=")([^"]*)(")',
    re.IGNORECASE,
)
STROKE_NUMBERS_GROUP_RE = re.compile(
    r'(<g\s+id="kvg:StrokeNumbers_[^"]+"\s+style=")([^"]*)(")',
    re.IGNORECASE,
)
TEXT_RE = re.compile(r"<text(\s[^>]*)>", re.IGNORECASE)

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def color_for_stroke(index: int, total: int) -> str:
    # Distribute hue from 0 (red) to 0.8 (violet) so we don't loop back to red on long kanji.
    if total <= 1:
        hue = 0.0
    else:
        hue = (index - 1) / max(total - 1, 1) * 0.78
    r, g, b = colorsys.hsv_to_rgb(hue, 0.78, 0.85)
    return f"#{int(r*255):02x}{int(g*255):02x}{int(b*255):02x}"


def enhance(svg: str) -> str:
    if MARKER in svg:
        return svg

    # Count strokes to know how to distribute the hue range
    stroke_indices = [int(m.group(2)) for m in PATH_RE.finditer(svg)]
    total = max(stroke_indices) if stroke_indices else 1

    # Beef up the StrokePaths group style — wider stroke, smoother caps
    def _strokes_style(match: re.Match[str]) -> str:
        existing = match.group(2)
        # remove explicit stroke color so per-path strokes win
        existing = re.sub(r"stroke:[^;]+;?", "", existing)
        # bump width slightly so strokes read on phone screens
        existing = re.sub(r"stroke-width:\s*\d+(\.\d+)?", "stroke-width:4", existing)
        if "stroke-width" not in existing:
            existing += "stroke-width:4;"
        if "stroke-linecap" not in existing:
            existing += "stroke-linecap:round;"
        if "stroke-linejoin" not in existing:
            existing += "stroke-linejoin:round;"
        if not existing.endswith(";"):
            existing += ";"
        return f"{match.group(1)}{existing}{match.group(3)}"

    svg = STROKE_PATHS_GROUP_RE.sub(_strokes_style, svg, count=1)

    # Per-stroke color
    def _path_color(match: re.Match[str]) -> str:
        head, idx_str, tail = match.group(1), match.group(2), match.group(3)
        color = color_for_stroke(int(idx_str), total)
        # avoid duplicating stroke="..." if it already exists
        if 'stroke="' in head:
            head_new = re.sub(r'stroke="[^"]*"', f'stroke="{color}"', head)
        else:
            head_new = head + f' stroke="{color}"'
        return f"{head_new}{tail}"

    svg = PATH_RE.sub(_path_color, svg)

    # Stroke number labels: bigger, bold, red
    def _numbers_style(match: re.Match[str]) -> str:
        existing = match.group(2)
        existing = re.sub(r"font-size:\s*\d+(\.\d+)?", "font-size:7", existing)
        existing = re.sub(r"fill:[^;]+;?", "", existing)
        existing += "fill:#dc2626;font-weight:bold;font-family:sans-serif;"
        return f"{match.group(1)}{existing}{match.group(3)}"

    svg = STROKE_NUMBERS_GROUP_RE.sub(_numbers_style, svg, count=1)

    # Add a small white halo behind each number so it's readable when the stroke runs underneath.
    # (Done by inserting `paint-order:stroke;stroke:#ffffff;stroke-width:1.2;` on text elements.)
    def _text_attrs(match: re.Match[str]) -> str:
        attrs = match.group(1)
        if "paint-order" not in attrs:
            attrs += ' style="paint-order:stroke;stroke:#ffffff;stroke-width:1.5;"'
        return f"<text{attrs}>"

    svg = TEXT_RE.sub(_text_attrs, svg)

    svg = svg.replace("<svg ", MARKER + "\n<svg ", 1)
    return svg


def main() -> int:
    files = sorted(SVG_DIR.glob("*.svg"))
    if not files:
        print("No SVGs found. Run download_kanjivg.py first.")
        return 1
    enhanced = 0
    skipped = 0
    for fp in files:
        text = fp.read_text(encoding="utf-8")
        new_text = enhance(text)
        if new_text != text:
            fp.write_text(new_text, encoding="utf-8")
            enhanced += 1
        else:
            skipped += 1
    print(f"Enhanced: {enhanced}, Already enhanced: {skipped}, Total: {len(files)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
