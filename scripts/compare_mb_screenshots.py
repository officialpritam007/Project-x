#!/usr/bin/env python3
"""Compare original and Liquid Chat screen CAPTURES pixel-by-pixel.

Never treats compiled APK XML, icons or invented mockups as reference screenshots.
No network, private media upload or image resizing. Requires Pillow.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from PIL import Image, ImageChops


def compare(reference: Path, candidate: Path, out: Path, tolerance: int,
            ignore_top: int, ignore_bottom: int) -> dict:
    with Image.open(reference) as source_file, Image.open(candidate) as candidate_file:
        source = source_file.convert("RGB")
        updated = candidate_file.convert("RGB")
    result = {"screen": reference.name, "reference_dimensions": list(source.size),
              "candidate_dimensions": list(updated.size)}
    if source.size != updated.size:
        result["error"] = "Pixel comparison impossible: resolutions differ; do not resize either image."
        return result

    width, height = source.size
    if ignore_top < 0 or ignore_bottom < 0 or ignore_top + ignore_bottom >= height:
        result["error"] = "Invalid excluded status/navigation bar region."
        return result
    delta = ImageChops.difference(source, updated)
    pixels = delta.load()
    changed = 0
    total_abs = 0
    max_delta = 0
    # Evaluate RGB channels of every valid pixel. Explicitly excluded
    # system-bar pixels are never counted as matching or mismatching.
    for y in range(ignore_top, height - ignore_bottom):
        for x in range(width):
            rgb = pixels[x, y]
            maximum = max(rgb)
            max_delta = max(max_delta, maximum)
            total_abs += sum(rgb)
            if maximum > tolerance:
                changed += 1
    compared = width * (height - ignore_top - ignore_bottom)
    prefix = reference.stem
    out.mkdir(parents=True, exist_ok=True)
    delta.save(out / f"{prefix}.difference.png")
    Image.blend(source, updated, 0.5).save(out / f"{prefix}.overlay.png")
    result.update({
        "compared_pixels": compared, "different_pixels": changed,
        "different_percent": round(100.0 * changed / compared, 6),
        "matched_percent": round(100.0 * (compared-changed) / compared, 6),
        "mean_absolute_rgb_difference": round(total_abs / (3 * compared), 6),
        "max_channel_difference": max_delta, "channel_tolerance": tolerance,
        "ignored_top_rows": ignore_top, "ignored_bottom_rows": ignore_bottom,
        "comparison": "pixel-by-pixel", "match": changed == 0
    })
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reference", required=True, type=Path,
                        help="Original APK actual device screenshots (not APK layouts)")
    parser.add_argument("--candidate", required=True, type=Path,
                        help="Liquid Chat actual device screenshots, same filenames")
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--tolerance", type=int, default=0,
                        help="Maximum per-channel difference counted as unchanged (0 for exact)")
    parser.add_argument("--ignore-top", type=int, default=0)
    parser.add_argument("--ignore-bottom", type=int, default=0)
    args = parser.parse_args()
    if not 0 <= args.tolerance <= 255:
        parser.error("--tolerance must be within 0..255")
    if not args.reference.is_dir() or not args.candidate.is_dir():
        parser.error("Both screenshot directories must exist")
    reference = {x.name: x for x in args.reference.glob("*.png") if x.is_file()}
    candidate = {x.name: x for x in args.candidate.glob("*.png") if x.is_file()}
    names = sorted(set(reference) | set(candidate))
    if not names:
        parser.error("No screenshot PNG pairs found; cannot assert parity using only an APK file.")
    report = []
    for name in names:
        if name not in reference or name not in candidate:
            report.append({"screen": name,
                           "error": "Missing original screenshot" if name not in reference
                                    else "Missing Liquid Chat screenshot"})
        else:
            report.append(compare(reference[name], candidate[name], args.output,
                                  args.tolerance, args.ignore_top, args.ignore_bottom))
    args.output.mkdir(parents=True, exist_ok=True)
    with (args.output / "report.json").open("w", encoding="utf-8") as file:
        json.dump({"screens": report, "reference_count": len(reference),
                   "candidate_count": len(candidate),
                   "all_matching": all(x.get("match", False) for x in report)}, file,
                  ensure_ascii=False, indent=2)
    for item in report:
        print(item["screen"] + ": " +
              (item["error"] if "error" in item else
               f'{item["matched_percent"]:.4f}% matching (' +
               f'{item["different_pixels"]} differing pixels)'))
    return 0 if all(x.get("match", False) for x in report) else 1


if __name__ == "__main__":
    raise SystemExit(main())
