param([Parameter(Mandatory=$true)][string]$Jdk)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
& "$root/meu-onibus-android/verification/run-checks.ps1" -Jdk $Jdk
& "$root/meu-onibus-android/verification/run-update-checks.ps1" -Jdk $Jdk
Push-Location "$root/meu-onibus"
try { & node check-suggestions.mjs; if($LASTEXITCODE -ne 0){throw 'Suggestions tests failed'}; & node check-app-release.mjs; if($LASTEXITCODE -ne 0){throw 'Release tests failed'}; & node check-audit.mjs; if($LASTEXITCODE -ne 0){throw 'Audit tests failed'}; & node check-itineraries.mjs; if($LASTEXITCODE -ne 0){throw 'Itinerary tests failed'}; & node build-api.mjs; if($LASTEXITCODE -ne 0){throw 'API build failed'} } finally { Pop-Location }
Write-Output 'PASS: all repository checks'
