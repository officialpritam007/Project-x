#!/usr/bin/env bash
set -euo pipefail
track="${1:-01-shell-home}"
root="$(git rev-parse --show-toplevel)"
manifest="$root/scripts/worktrees/tracks.json"
base="$(dirname "$root")/$(basename "$root")-worktrees"
# Resolve/validate selection before doing any repository mutation.
selection="$(python3 - "$manifest" "$track" <<'PY'
import json,sys
data=json.load(open(sys.argv[1],encoding="utf-8"))
choice=sys.argv[2]
selected=[t for t in data["tracks"] if choice=="all" or t["folder"]==choice]
if not selected: raise SystemExit("Unknown worktree track: "+choice)
for t in selected: print(t["folder"]+"|"+t["branch"])
PY
)"
mkdir -p "$base"
git -C "$root" fetch origin --prune
while IFS='|' read -r folder branch; do
  [[ -n "$folder" ]] || continue
  git -C "$root" show-ref --verify --quiet "refs/remotes/origin/$branch" || { echo "Missing origin/$branch" >&2; exit 1; }
  if ! git -C "$root" show-ref --verify --quiet "refs/heads/$branch"; then
    git -C "$root" branch --track "$branch" "origin/$branch"
  fi
  if [[ -e "$base/$folder" ]]; then
    echo "Existing path untouched: $base/$folder"
    continue
  fi
  git -C "$root" worktree add "$base/$folder" "$branch"
done <<< "$selection"
git -C "$root" worktree list
