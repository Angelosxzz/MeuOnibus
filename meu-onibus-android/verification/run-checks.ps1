param([Parameter(Mandatory=$true)][string]$Jdk)
$ErrorActionPreference='Stop'
$javac=Join-Path $Jdk 'bin/javac'
$java=Join-Path $Jdk 'bin/java'
if(Test-Path -LiteralPath "$javac.exe"){$javac+='.exe'}
if(Test-Path -LiteralPath "$java.exe"){$java+='.exe'}
$projectRoot=Split-Path -Parent $PSScriptRoot
$buildRoot=Join-Path $projectRoot 'build-manual/verification'
New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null
& $javac -encoding UTF-8 -d $buildRoot "$projectRoot/app/src/main/java/br/com/meuonibus/Schedule.java" "$projectRoot/app/src/main/java/br/com/meuonibus/RoadForecast.java" "$PSScriptRoot/RoadForecastCheck.java"
if($LASTEXITCODE -ne 0){throw 'Falha ao compilar verificações'}
& $java -cp $buildRoot br.com.meuonibus.RoadForecastCheck
if($LASTEXITCODE -ne 0){throw 'Falha no cálculo de previsão'}
& $javac -encoding UTF-8 -d $buildRoot "$projectRoot/../shared/src/main/java/br/com/meuonibus/core/MapUrlPolicy.java" "$projectRoot/../shared/verification/MapUrlPolicyCheck.java"
if($LASTEXITCODE -ne 0){throw 'Falha ao compilar política do mapa'}
& $java -cp $buildRoot br.com.meuonibus.core.MapUrlPolicyCheck
if($LASTEXITCODE -ne 0){throw 'Falha na política do mapa'}
& $javac -encoding UTF-8 -d $buildRoot "$projectRoot/../shared/src/main/java/br/com/meuonibus/core/ItineraryPolicy.java" "$projectRoot/../shared/verification/ItineraryPolicyCheck.java"
if($LASTEXITCODE -ne 0){throw 'Falha ao compilar política do itinerário'}
& $java -cp $buildRoot br.com.meuonibus.core.ItineraryPolicyCheck
if($LASTEXITCODE -ne 0){throw 'Falha na política do itinerário'}
