<# 
.SYNOPSIS
    Build and package the Dalian Da Gunzi application.

.DESCRIPTION
    Uses Maven to build the project.
    This script should be called from the project root directory.
    Tests are skipped by default; use -RunTests to include them.
#>
param(
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'

# Build command (skip tests by default)
$mvnArgs = @('clean', 'package')
if (-not $RunTests) {
    $mvnArgs += '-DskipTests'
}

Write-Host "Building with Maven..." -ForegroundColor Cyan
Write-Host "  Arguments: $($mvnArgs -join ' ')" -ForegroundColor Gray

& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE"
}

Write-Host "Build completed successfully." -ForegroundColor Green
