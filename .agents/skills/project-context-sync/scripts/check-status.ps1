# Script to quickly inspect the repository health and state without external gitconfig interference
$env:GIT_CONFIG_GLOBAL='NUL'
$env:GIT_CONFIG_SYSTEM='NUL'

Write-Host "=== Current Git Branch & Status ==="
git status -s

Write-Host "`n=== Last 3 Commits ==="
git log -n 3 --oneline

Write-Host "`n=== Context File Verification ==="
if (Test-Path "antigravity/project-context.md") {
    Write-Host "antigravity/project-context.md exists."
} else {
    Write-Warning "antigravity/project-context.md NOT found!"
}
