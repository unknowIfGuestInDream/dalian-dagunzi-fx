<# 
.SYNOPSIS
    Build and package the Dalian Da Gunzi application.

.DESCRIPTION
    Uses Gradle to build the project.
    This script should be called from the project root directory.
    Tests are skipped by default; use -RunTests to include them.
#>
param(
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'

# Build command (assemble the app image; skip tests by default)
$gradleArgs = @('clean', 'appImage', '--no-daemon')
if ($RunTests) {
    $gradleArgs += 'test'
}

Write-Host "Building with Gradle..." -ForegroundColor Cyan
Write-Host "  Arguments: $($gradleArgs -join ' ')" -ForegroundColor Gray

& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE"
}

Write-Host "Build completed successfully." -ForegroundColor Green
