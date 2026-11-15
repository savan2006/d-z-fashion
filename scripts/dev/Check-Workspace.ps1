$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Write-Host 'Checking Java...' -ForegroundColor Cyan
java -version
Write-Host 'Checking Maven wrapper...' -ForegroundColor Cyan
Push-Location (Join-Path $root 'backend')
try { .\mvnw.cmd -version } finally { Pop-Location }
Write-Host 'Checking Node and npm...' -ForegroundColor Cyan
node --version
npm --version
Write-Host 'Workspace toolchain check completed.' -ForegroundColor Green
