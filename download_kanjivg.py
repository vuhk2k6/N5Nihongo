"""Download KanjiVG SVG stroke-order diagrams for the characters used in this app.

This pulls SVGs straight from the KanjiVG GitHub repo. The SVGs include numbered
stroke groups (kvg:StrokeNumbers-*) which we'll show in-app via Coil's SVG decoder.

Run once: `python download_kanjivg.py`. Output is written to
`app/src/main/assets/kanjivg/<5-digit-hex-codepoint>.svg`.
"""

from __future__ import annotations

import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

ROOT = Path(__file__).resolve().parent
LESSONS_DIR = ROOT / "app" / "src" / "main" / "assets" / "lessons"
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "kanjivg"
BASE_URL = "https://raw.githubusercontent.com/KanjiVG/kanjivg/master/kanji/{name}.svg"

# Re-encode stdout as UTF-8 so we can print Japanese characters on Windows consoles.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def hiragana_range() -> list[str]:
    # U+3041..U+3096 covers all basic hiragana including small kana.
    return [chr(c) for c in range(0x3041, 0x3097)]


def katakana_range() -> list[str]:
    # U+30A1..U+30FA covers all basic katakana including small kana.
    return [chr(c) for c in range(0x30A1, 0x30FB)]


def kanji_from_lessons() -> set[str]:
    chars: set[str] = set()
    for fp in sorted(LESSONS_DIR.glob("lesson_*.json")):
        data = json.loads(fp.read_text(encoding="utf-8"))
        for entry in data.get("kanji", []) or []:
            ch = entry.get("character", "")
            if ch:
                chars.add(ch)
        # also include kanji used inside vocabulary so the kanji detail still works for
        # words like 學, 寝, etc. that appear in furigana/kanji fields
        for entry in data.get("vocabulary", []) or []:
            for field in ("kanji", "furigana"):
                text = entry.get(field, "")
                for ch in text:
                    cp = ord(ch)
                    if 0x4E00 <= cp <= 0x9FFF:
                        chars.add(ch)
    return chars


def codepoint_filename(ch: str) -> str:
    return f"{ord(ch):05x}"


def download_one(ch: str) -> tuple[str, str]:
    name = codepoint_filename(ch)
    out_path = OUT_DIR / f"{name}.svg"
    if out_path.exists() and out_path.stat().st_size > 200:
        return ch, "cached"
    url = BASE_URL.format(name=name)
    req = Request(url, headers={"User-Agent": "n5nihongo-download/1.0"})
    try:
        with urlopen(req, timeout=20) as resp:
            data = resp.read()
        out_path.write_bytes(data)
        return ch, "ok"
    except HTTPError as e:
        if e.code == 404:
            return ch, "missing"
        return ch, f"http {e.code}"
    except (URLError, TimeoutError) as e:
        return ch, f"err {e}"


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    chars: list[str] = []
    chars.extend(hiragana_range())
    chars.extend(katakana_range())
    chars.extend(sorted(kanji_from_lessons()))
    # de-dup while preserving order
    seen: set[str] = set()
    chars = [c for c in chars if not (c in seen or seen.add(c))]

    print(f"Total characters to fetch: {len(chars)}")
    print(f"Output: {OUT_DIR}")

    start = time.time()
    results: dict[str, list[str]] = {"ok": [], "cached": [], "missing": [], "error": []}
    # KanjiVG isn't a heavy host; 8 parallel connections is gentle and finishes ~30s.
    with ThreadPoolExecutor(max_workers=8) as ex:
        futures = {ex.submit(download_one, ch): ch for ch in chars}
        done = 0
        for fut in as_completed(futures):
            ch, status = fut.result()
            done += 1
            bucket = status if status in ("ok", "cached", "missing") else "error"
            results[bucket].append(ch)
            if done % 20 == 0 or done == len(chars):
                print(f"  {done}/{len(chars)} done…")

    elapsed = time.time() - start
    print(
        f"\nDone in {elapsed:.1f}s — "
        f"new={len(results['ok'])} cached={len(results['cached'])} "
        f"missing={len(results['missing'])} error={len(results['error'])}"
    )
    if results["missing"]:
        print("Missing on KanjiVG:", "".join(results["missing"]))
    if results["error"]:
        print("Errors:", "".join(results["error"][:50]))
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
