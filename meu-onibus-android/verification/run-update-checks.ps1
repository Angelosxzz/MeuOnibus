param([Parameter(Mandatory=$true)][string]$Jdk)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$buildRoot=Join-Path $projectRoot 'build-manual/update-verification'
New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null
& "$Jdk/bin/javac.exe" -encoding UTF-8 -d $buildRoot "$projectRoot/app/src/main/java/br/com/meuonibus/UpdatePolicy.java" "$PSScriptRoot/UpdatePolicyCheck.java"
if($LASTEXITCODE -ne 0){throw 'Falha ao compilar verificações'}
& "$Jdk/bin/java.exe" -cp $buildRoot br.com.meuonibus.UpdatePolicyCheck
if($LASTEXITCODE -ne 0){throw 'Falha no controle de atualização'}
