[CmdletBinding()]
param(
    [string]$EnvironmentFile = (Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) '.env')
)
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
}
Write-Host "Loaded local environment variables from $resolved" -ForegroundColor Green
