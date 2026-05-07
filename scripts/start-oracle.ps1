$ErrorActionPreference = "Stop"

$containerName = "cs2-oracle"

$exists = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $containerName }
if (-not $exists) {
    throw "Oracle container '$containerName' does not exist. Recreate it with the setup command from README.md."
}

$running = docker inspect --format "{{.State.Running}}" $containerName
if ($running -ne "true") {
    docker start $containerName | Out-Null
}

$deadline = (Get-Date).AddMinutes(5)
do {
    $port = Get-NetTCPConnection -LocalPort 1521 -State Listen -ErrorAction SilentlyContinue
    if ($port) {
        Write-Host "Oracle listener is available on localhost:1521."
        exit 0
    }
    Write-Host "Waiting for Oracle listener on localhost:1521..."
    Start-Sleep -Seconds 5
} while ((Get-Date) -lt $deadline)

docker logs --tail 80 $containerName
throw "Oracle listener did not become available on localhost:1521."
