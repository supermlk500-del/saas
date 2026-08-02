[CmdletBinding()]
param(
    [string]$ConfigPath = $env:REDIS_CONFIG_PATH
)

$ErrorActionPreference = 'Stop'

function Test-LocalRedis {
    $reply = & redis-cli -h 127.0.0.1 -p 6379 ping 2>$null
    return $LASTEXITCODE -eq 0 -and $reply -match 'PONG'
}

$redisServer = (Get-Command redis-server -ErrorAction Stop).Source
Get-Command redis-cli -ErrorAction Stop | Out-Null

if (Test-LocalRedis) {
    Write-Host 'Redis is already available at 127.0.0.1:6379.'
    exit 0
}

if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
    $defaultConfig = Join-Path (Split-Path $redisServer -Parent) 'redis.windows-service.conf'
    if (Test-Path -LiteralPath $defaultConfig) {
        $ConfigPath = $defaultConfig
    }
}

$arguments = if (-not [string]::IsNullOrWhiteSpace($ConfigPath)) {
    if (-not (Test-Path -LiteralPath $ConfigPath)) {
        throw "Redis config does not exist: $ConfigPath"
    }
    @($ConfigPath)
} else {
    @('--bind', '127.0.0.1', '--port', '6379', '--appendonly', 'no')
}

Start-Process -FilePath $redisServer -ArgumentList $arguments -WindowStyle Hidden

for ($attempt = 0; $attempt -lt 20; $attempt++) {
    Start-Sleep -Milliseconds 250
    if (Test-LocalRedis) {
        Write-Host 'Redis started successfully at 127.0.0.1:6379.'
        exit 0
    }
}

throw 'Redis failed to start or requires authentication. Check the Redis configuration and REDIS_PASSWORD.'
