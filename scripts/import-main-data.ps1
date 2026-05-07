$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$schemaPath = Join-Path $projectRoot "sql\schema.sql"
$datasetPath = Join-Path $projectRoot "Milestone2Files\mega_dataset.sql"
$containerName = "cs2-oracle"
$dbUser = "CS2"
$dbPassword = "cs2_password"
$dbService = "localhost:1521/FREEPDB1"

if ($args.Count -ge 1 -and -not [string]::IsNullOrWhiteSpace($args[0])) {
    $datasetPath = $args[0]
}

if (-not (Test-Path -LiteralPath $schemaPath)) {
    throw "Schema file not found: $schemaPath"
}

if (-not (Test-Path -LiteralPath $datasetPath)) {
    throw "Dataset file not found: $datasetPath"
}

& (Join-Path $PSScriptRoot "start-oracle.ps1")

docker cp $schemaPath "${containerName}:/tmp/cs2_schema.sql"
docker cp $datasetPath "${containerName}:/tmp/cs2_dataset.sql"

$schemaWrapper = Join-Path $env:TEMP "cs2_schema_wrapper.sql"
$datasetWrapper = Join-Path $env:TEMP "cs2_dataset_wrapper.sql"

@"
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET ECHO OFF
SET FEEDBACK OFF
@/tmp/cs2_schema.sql
EXIT
"@ | Set-Content -Encoding ASCII -LiteralPath $schemaWrapper

@"
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET ECHO OFF
SET FEEDBACK OFF
@/tmp/cs2_dataset.sql
EXIT
"@ | Set-Content -Encoding ASCII -LiteralPath $datasetWrapper

docker cp $schemaWrapper "${containerName}:/tmp/cs2_schema_wrapper.sql"
docker cp $datasetWrapper "${containerName}:/tmp/cs2_dataset_wrapper.sql"

docker exec $containerName bash -lc "sqlplus -S -L $dbUser/$dbPassword@$dbService @/tmp/cs2_schema_wrapper.sql"
docker exec $containerName bash -lc "sqlplus -S -L $dbUser/$dbPassword@$dbService @/tmp/cs2_dataset_wrapper.sql"

Write-Host "Imported dataset: $datasetPath"
