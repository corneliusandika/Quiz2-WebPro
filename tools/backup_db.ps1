param(
    [string]$DbHost = "",
    [string]$DbPort = "",
    [string]$DbName = "",
    [string]$DbUser = "",
    [string]$DbPassword = "",
    [string]$OutputDir = "",
    [int]$KeepDays = 30
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($DbHost)) { $DbHost = if ($env:DOMPETKU_DB_HOST) { $env:DOMPETKU_DB_HOST } else { "127.0.0.1" } }
if ([string]::IsNullOrWhiteSpace($DbPort)) { $DbPort = if ($env:DOMPETKU_DB_PORT) { $env:DOMPETKU_DB_PORT } else { "3306" } }
if ([string]::IsNullOrWhiteSpace($DbName)) { $DbName = if ($env:DOMPETKU_DB_NAME) { $env:DOMPETKU_DB_NAME } else { "dompetku" } }
if ([string]::IsNullOrWhiteSpace($DbUser)) { $DbUser = if ($env:DOMPETKU_DB_USER) { $env:DOMPETKU_DB_USER } else { "root" } }
if ([string]::IsNullOrWhiteSpace($DbPassword)) { $DbPassword = $env:DOMPETKU_DB_PASSWORD }
if ([string]::IsNullOrWhiteSpace($OutputDir)) { $OutputDir = Join-Path (Split-Path -Parent $PSScriptRoot) "backups" }

$dumpCandidates = @(
    $env:DOMPETKU_MYSQLDUMP,
    "C:\xampp\mysql\bin\mysqldump.exe",
    "mysqldump"
) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

$dump = $null
foreach ($candidate in $dumpCandidates) {
    if ($candidate -eq "mysqldump") {
        $command = Get-Command mysqldump -ErrorAction SilentlyContinue
        if ($command) { $dump = $command.Source; break }
    } elseif (Test-Path -LiteralPath $candidate) {
        $dump = $candidate
        break
    }
}
if (-not $dump) {
    throw "mysqldump tidak ditemukan. Set DOMPETKU_MYSQLDUMP ke lokasi mysqldump."
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$output = Join-Path $OutputDir ("dompetku-{0}.sql" -f $stamp)

$tempConfig = [System.IO.Path]::GetTempFileName()
try {
    @(
        "[client]",
        "user=$DbUser",
        "password=$DbPassword",
        "host=$DbHost",
        "port=$DbPort",
        "default-character-set=utf8mb4"
    ) | Set-Content -LiteralPath $tempConfig -Encoding ASCII

    & $dump "--defaults-extra-file=$tempConfig" "--single-transaction" "--routines" "--triggers" `
        "--databases" $DbName "--result-file=$output"
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $output)) {
        throw "mysqldump gagal (exit code $LASTEXITCODE)."
    }
    Write-Host "Backup dibuat: $output"
} finally {
    Remove-Item -LiteralPath $tempConfig -Force -ErrorAction SilentlyContinue
}

Get-ChildItem -LiteralPath $OutputDir -Filter "dompetku-*.sql" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$KeepDays) } |
    Remove-Item -Force

Write-Host "Retensi: file lebih lama dari $KeepDays hari dibersihkan."
