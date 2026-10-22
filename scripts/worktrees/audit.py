#!/usr/bin/env python3
"""No-regression ledger check: stable IDs, monotonic count, manifest uniqueness."""
from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[2]
manifest=json.loads((root/"scripts/worktrees/tracks.json").read_text(encoding="utf-8"))
matrix=(root/"docs/mb-parity/FEATURE_MATRIX.md").read_text(encoding="utf-8")
tracks=manifest["tracks"]
branches=[t["branch"] for t in tracks]
assert len(branches)==len(set(branches)), "Duplicate branch names"
assert len(tracks)==12, "A workstream disappeared"
actual={}
for t in tracks:
    id_=t["id"]
    ids=re.findall(r"`P"+id_+r"-(\d{3})`",matrix)
    assert ids,"No inventory in track "+id_
    numbers=list(map(int,ids))
    assert numbers==list(range(1,len(numbers)+1)), "Missing/duplicate/out-of-order IDs in "+id_
    actual[id_]=len(ids)
observed=sum(actual.values())
assert observed>=manifest["items"], f"Inventory shrunk: {observed} < {manifest['items']}"
print(f"OK: {observed} stable items / {len(tracks)} worktrees; by track: {actual}")
