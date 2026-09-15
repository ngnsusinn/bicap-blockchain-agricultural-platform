# ==========================================================================
# Nạp .env (production config) vào môi trường của tiến trình rồi chạy backend.
#
#   .\dev\run-backend.ps1
#
# Backend KHÔNG tự đọc .env (giống mọi app Spring Boot thật), nên script này
# export toàn bộ biến trong .env trước khi gọi `mvn spring-boot:run`.
# Không còn secret sinh tạm: thiếu key ⇒ backend dừng khởi động ngay.
# ==========================================================================
[CmdletBinding()]
param(
    [string]$EnvFile = (Join-Path (Split-Path -Parent $PSScriptRoot) '.env'),
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not (Test-Path $EnvFile)) {
    throw "Không thấy $EnvFile — tạo file này từ .env.example rồi điền key thật."
}

$loaded = 0
foreach ($raw in Get-Content $EnvFile) {
    $line = $raw.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith('#')) { continue }
    $idx = $line.IndexOf('=')
    if ($idx -lt 1) { continue }

    $name = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1).Trim()
    # Bỏ dấu nháy bao ngoài (nếu có) — giá trị có '&' không cần quote khi set qua .NET.
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    $loaded++
}

Write-Host "Đã nạp $loaded biến từ $EnvFile" -ForegroundColor Green
Write-Host ("MYSQL  -> {0}" -f ($env:SPRING_DATASOURCE_URL -replace '\?.*$', '')) -ForegroundColor DarkGray
Write-Host ("REDIS  -> {0}:{1}" -f $env:SPRING_REDIS_HOST, $env:SPRING_REDIS_PORT) -ForegroundColor DarkGray
Write-Host ("CHAIN  -> mode={0} export={1}" -f $env:BLOCKCHAIN_MODE, $env:BLOCKCHAIN_EXPORT_MODE) -ForegroundColor DarkGray

Push-Location (Join-Path $repoRoot 'backend')
try {
    if ($SkipTests) {
        & mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.main.lazy-initialization=false"
    } else {
        & mvn spring-boot:run
    }
} finally {
    Pop-Location
}
