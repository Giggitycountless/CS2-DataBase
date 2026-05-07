$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot "src\main\java"
$outputRoot = Join-Path $projectRoot "out\classes"
$resourceRoot = Join-Path $projectRoot "src\main\resources"

New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

$sources = Get-ChildItem -Path $sourceRoot -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
if (-not $sources) {
    throw "No Java source files found."
}

javac -encoding UTF-8 -d $outputRoot $sources

if (Test-Path $resourceRoot) {
    Copy-Item -Path (Join-Path $resourceRoot "*") -Destination $outputRoot -Recurse -Force
}

Write-Host "Compiled classes to $outputRoot"
