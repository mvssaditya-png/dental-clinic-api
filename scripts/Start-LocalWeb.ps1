# Run in your own interactive PowerShell terminal. Never redirect/transcribe this session.
[CmdletBinding()]
param([switch]$LocalOtpConsole)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $repo 'target/dental-clinic-api-0.0.1-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $jar)) { throw 'Build first: .\mvnw.cmd -DskipTests package' }
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Port 8080 is occupied. Stop the existing backend in its IDE/terminal, then retry.'
}
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java.exe -ErrorAction Stop).Source }
# Preserve a dedicated key in this trusted terminal across restarts, never in a file.
if (!$env:DENTAL_WEB_CSRF_KEY) {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes); $env:DENTAL_WEB_CSRF_KEY = [Convert]::ToBase64String($bytes) }
    finally { $rng.Dispose(); [Array]::Clear($bytes,0,$bytes.Length) }
}
$options = @(
    '-jar', $jar,
    '--spring.profiles.active=local-web',
    '--spring.datasource.url=jdbc:postgresql://localhost:5432/dental_clinic_test_db',
    '--server.address=127.0.0.1', '--server.port=8080',
    '--app.web-session.enabled=true', '--app.web-session.secure=false',
    '--app.web-session.origins=http://localhost:8081',
    '--spring.flyway.enabled=true'
)
if ($LocalOtpConsole) { $options += '--app.local-otp-console.enabled=true' }
else { $options += '--app.local-otp-console.enabled=false' }
Write-Host 'Local-only backend: http://127.0.0.1:8080 | Browser: http://localhost:8081'
Write-Host 'Database: dental_clinic_test_db | Profile: local-web | Secrets stay in terminal memory.'
if ($LocalOtpConsole) { Write-Host 'OTP delivery uses this interactive console only. Do not redirect output or enable transcription.' }
Push-Location $repo
try { & $java @options; if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" } }
finally { Pop-Location }
