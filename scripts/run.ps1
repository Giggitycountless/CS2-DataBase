$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$classes = Join-Path $projectRoot "out\classes"
$lib = Join-Path $projectRoot "lib\*"
$mavenDependencies = Join-Path $projectRoot "target\dependency\*"

if (-not (Test-Path $classes)) {
    & (Join-Path $PSScriptRoot "build.ps1")
}

$classpath = "$classes;$lib;$mavenDependencies"
java -cp $classpath com.counterstrike.app.CounterStrikeApp
