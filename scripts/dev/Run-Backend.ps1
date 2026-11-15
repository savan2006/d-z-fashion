[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('api-gateway','identity-service','catalog-service','inventory-service','commerce-service','payment-service','fulfillment-service')]
    [string]$Module
)
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
. (Join-Path $PSScriptRoot 'Import-ProjectEnv.ps1') -EnvironmentFile (Join-Path $root '.env')
Push-Location (Join-Path $root 'backend')
try {
    .\mvnw.cmd -pl "modules/$Module" -am spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Maven exited with code $LASTEXITCODE" }
}
finally { Pop-Location }
