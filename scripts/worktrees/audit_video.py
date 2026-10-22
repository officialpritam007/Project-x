#!/usr/bin/env python3
"""Static video-to-inventory traceability validation; does not build Android."""
from pathlib import Path
import json, re
root = Path(__file__).resolve().parents[2]
manifest = json.loads((root / "scripts/worktrees/tracks.json").read_text(encoding="utf-8"))
matrix = (root / "docs/mb-parity/FEATURE_MATRIX.md").read_text(encoding="utf-8")
crosswalk = json.loads((root / "docs/mb-parity/VIDEO_CROSSWALK.json").read_text(encoding="utf-8"))
ids = re.findall(r"\`(P\d\d-\d{3})\`", matrix)
assert len(ids) == len(set(ids)), "Duplicate feature IDs"
assert len(ids) >= manifest["items"], "Master inventory unexpectedly shrank"
assert len(crosswalk["rows"]) >= 20, "Missing reference scene"
assert {x["source"] for x in crosswalk["rows"]} == {"R1", "R2"}
known, owners = set(ids), {t["id"] for t in manifest["tracks"]}
for row in crosswalk["rows"]:
    assert row["featureIds"], "Unassigned scene: " + row["id"]
    assert len(row["featureIds"]) == len(set(row["featureIds"])), "Repeated scene ID"
    for key in row["featureIds"]:
        assert key in known, "Unknown feature: " + key
        assert key[1:3] in owners, "Unowned feature: " + key
print("Validated", len(crosswalk["rows"]), "scenes and",
      len({x for row in crosswalk["rows"] for x in row["featureIds"]}), "distinct mapped IDs")
