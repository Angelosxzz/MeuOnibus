param([Parameter(Mandatory=$true)][string]$Jdk)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$buildRoot=Join-Path $projectRoot 'build-manual/verification'
New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null
& "$Jdk/bin/javac.exe" -encoding UTF-8 -d $buildRoot "$projectRoot/app/src/main/java/br/com/meuonibus/Schedule.java" "$projectRoot/app/src/main/java/br/com/meuonibus/RoadForecast.java" "$PSScriptRoot/RoadForecastCheck.java"
if($LASTEXITCODE -ne 0){throw 'Falha ao compilar verificações'}
& "$Jdk/bin/java.exe" -cp $buildRoot br.com.meuonibus.RoadForecastCheck
if($LASTEXITCODE -ne 0){throw 'Falha no cálculo de previsão'}
