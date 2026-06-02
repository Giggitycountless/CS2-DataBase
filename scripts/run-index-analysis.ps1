# run-index-analysis.ps1
# Runs the full Milestone 3 index performance analysis in three steps.
# Results are spooled to sql/indexes/*.txt for copy-paste into the report.
#
# Prerequisites: Oracle container cs2-oracle must be running.
# Usage: .\scripts\run-index-analysis.ps1

$ErrorActionPreference = "Stop"

$container = "cs2-oracle"
$connStr   = "CS2/cs2_password@localhost:1521/FREEPDB1"
$indexDir  = "sql\indexes"

function Run-Step {
    param([string]$Label, [string]$ScriptFile)
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "  $Label" -ForegroundColor Cyan
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan

    $localPath  = Join-Path $PSScriptRoot "..\$indexDir\$ScriptFile"
    $remotePath = "/tmp/$ScriptFile"

    docker cp $localPath "${container}:${remotePath}"
    docker exec $container bash -lc "sqlplus -L $connStr @$remotePath"
}

# Ensure Oracle is up
Write-Host "Starting Oracle container..." -ForegroundColor Yellow
docker start $container 2>$null
Start-Sleep -Seconds 3

# Step 0 – baseline (no indexes)
Run-Step "Step 0 – Baseline (no extra indexes)" "baseline.sql"

# Step 1 – B-Tree indexes
Run-Step "Step 1 – B-Tree indexes" "btree_indexes.sql"

# Step 2 – Hash clusters
Run-Step "Step 2 – Hash clusters" "hash_clusters.sql"

Write-Host ""
Write-Host "All steps complete." -ForegroundColor Green
Write-Host "Spool files (baseline_results.txt, btree_results.txt, hash_cluster_results.txt)"
Write-Host "are inside the container at /tmp/. Copy them out with:"
Write-Host "  docker cp ${container}:/tmp/baseline_results.txt     sql\indexes\"
Write-Host "  docker cp ${container}:/tmp/btree_results.txt        sql\indexes\"
Write-Host "  docker cp ${container}:/tmp/hash_cluster_results.txt sql\indexes\"
