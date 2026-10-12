param([Parameter(Mandatory=$true)][string]$Jdk)
$ErrorActionPreference='Stop'
$javac=Join-Path $Jdk 'bin/javac'
$java=Join-Path $Jdk 'bin/java'
if(Test-Path -LiteralPath "$javac.exe"){$javac+='.exe'}
if(Test-Path -LiteralPath "$java.exe"){$java+='.exe'}
$projectRoot=Split-Path -Parent $PSScriptRoot
$buildRoot=Join-Path $projectRoot 'build-manual/update-verification'
New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null
& $javac -encoding UTF-8 -d $buildRoot "$projectRoot/app/src/main/java/br/com/meuonibus/UpdatePolicy.java" "$PSScriptRoot/UpdatePolicyCheck.java"
if($LASTEXITCODE -ne 0){throw 'Falha ao compilar verificações'}
& $java -cp $buildRoot br.com.meuonibus.UpdatePolicyCheck
if($LASTEXITCODE -ne 0){throw 'Falha no controle de atualização'}
