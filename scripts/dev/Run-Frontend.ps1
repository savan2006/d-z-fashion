[CmdletBinding()]
param()
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
. (Join-Path $PSScriptRoot 'Import-ProjectEnv.ps1') -EnvironmentFile (Join-Path $root '.env')
Push-Location (Join-Path $root 'frontend')
try {
    if (-not (Test-Path 'node_modules')) {
        if (Test-Path 'package-lock.json') { npm ci } else { npm install }
    }
    npm run dev
}
finally { Pop-Location }
