param(
  [string]$Track = "01-shell-home",
  [switch]$All,
  [string]$RepoPath = "."
)
$ErrorActionPreference = "Stop"
$root = (& git -C $RepoPath rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0 -or -not $root) { throw "Run this script inside the Project-x Git checkout." }
$manifestPath = Join-Path $root "scripts/worktrees/tracks.json"
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$worktreeRoot = Join-Path (Split-Path $root -Parent) ((Split-Path $root -Leaf) + "-worktrees")
New-Item -ItemType Directory -Path $worktreeRoot -Force | Out-Null
& git -C $root fetch origin --prune
if ($LASTEXITCODE -ne 0) { throw "Unable to fetch remote branches. No worktree changed." }
$selected = @($manifest.tracks | Where-Object { $All -or $_.folder -eq $Track })
if ($selected.Count -eq 0) { throw "Unknown track '$Track'. See scripts/worktrees/tracks.json" }
foreach ($t in $selected) {
  $path = Join-Path $worktreeRoot $t.folder
  & git -C $root show-ref --verify --quiet ("refs/remotes/origin/" + $t.branch)
  if ($LASTEXITCODE -ne 0) { throw ("Missing remote branch: " + $t.branch + ". Initialize branches in GitHub first.") }
  $local = & git -C $root branch --list --format="%(refname:short)" $t.branch
  if ($LASTEXITCODE -ne 0) { throw "Cannot query local branches." }
  if (-not $local) {
    & git -C $root branch --track $t.branch ("origin/" + $t.branch)
    if ($LASTEXITCODE -ne 0) { throw ("Unable to track " + $t.branch) }
  }
  if (Test-Path -LiteralPath $path) {
    Write-Host ("Exists, not overwritten: " + $path)
    continue
  }
  & git -C $root worktree add $path $t.branch
  if ($LASTEXITCODE -ne 0) { throw ("Unable to create " + $path) }
  Write-Host ("Created: " + $path + " (" + $t.branch + ")")
}
Write-Host "Done. Inspect via: git worktree list"
