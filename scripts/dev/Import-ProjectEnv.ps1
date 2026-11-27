[CmdletBinding()]
param(
    [string]$EnvironmentFile
)

if (-not $EnvironmentFile) {
    $scriptDir = if ($PSScriptRoot) { $PSScriptRoot } elseif ($MyInvocation.MyCommand.Path) { Split-Path -Parent $MyInvocation.MyCommand.Path } else { $null }
    if ($scriptDir) {
        $rootDir = Split-Path -Parent (Split-Path -Parent $scriptDir)
        $EnvironmentFile = Join-Path $rootDir '.env'
    } else {
        $EnvironmentFile = Join-Path (Get-Location) '.env'
    }
}

$resolved = [System.IO.Path]::GetFullPath($EnvironmentFile)
if (-not (Test-Path -LiteralPath $resolved)) {
    throw "Environment file not found: $resolved. Copy .env.example to .env and fill local values."
}
Get-Content -LiteralPath $resolved | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) { return }
    $separator = $line.IndexOf('=')
    if ($separator -lt 1) { return }
    $name = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    Set-Item -Path "env:$name" -Value $value
}
Write-Host "Loaded local environment variables from $resolved" -ForegroundColor Green
