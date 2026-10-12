param([Parameter(Mandatory=$true)][string]$Jdk,[Parameter(Mandatory=$true)][string]$Platform,[Parameter(Mandatory=$true)][string]$BuildTools,[Parameter(Mandatory=$true)][string]$Keystore)
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$buildRoot=Join-Path $projectRoot 'build-manual'
$env:JAVA_HOME=$Jdk
New-Item -ItemType Directory -Force -Path "$buildRoot\gen","$buildRoot\classes","$buildRoot\dex" | Out-Null
[xml]$compiledManifest=Get-Content -LiteralPath "$projectRoot\app\src\main\AndroidManifest.xml" -Raw
$compiledManifest.DocumentElement.SetAttribute('package','br.com.meuonibus')
$compiledManifest.OuterXml | Set-Content -LiteralPath "$buildRoot\AndroidManifest.xml" -Encoding utf8
function Invoke-Checked([string]$Program,[string[]]$Arguments){ & $Program @Arguments; if($LASTEXITCODE -ne 0){ throw "Falha na compilação: $Program (código $LASTEXITCODE)" } }
Invoke-Checked "$BuildTools\aapt2.exe" @('compile','--dir',"$projectRoot\app\src\main\res",'-o',"$buildRoot\resources.zip")
Invoke-Checked "$BuildTools\aapt2.exe" @('link','-o',"$buildRoot\resources.apk",'--manifest',"$buildRoot\AndroidManifest.xml",'-I',"$Platform\android.jar",'--java',"$buildRoot\gen",'-A',"$projectRoot\app\src\main\assets",'--min-sdk-version','26','--target-sdk-version','36',"$buildRoot\resources.zip")
$sources=@(Get-ChildItem -LiteralPath "$projectRoot\app\src\main\java","$buildRoot\gen" -Filter '*.java' -Recurse | ForEach-Object FullName)
Invoke-Checked "$Jdk\bin\javac.exe" (@('-encoding','UTF-8','-source','17','-target','17','-classpath',"$Platform\android.jar",'-d',"$buildRoot\classes")+$sources)
Invoke-Checked "$Jdk\bin\jar.exe" @('cf',"$buildRoot\classes.jar",'-C',"$buildRoot\classes",'.')
Invoke-Checked "$Jdk\bin\java.exe" @('-cp',"$BuildTools\lib\d8.jar",'com.android.tools.r8.D8','--lib',"$Platform\android.jar",'--min-api','26','--output',"$buildRoot\dex", "$buildRoot\classes.jar")
Copy-Item -LiteralPath "$buildRoot\resources.apk" -Destination "$buildRoot\unsigned.apk" -Force
Invoke-Checked "$Jdk\bin\jar.exe" @('uf',"$buildRoot\unsigned.apk",'-C',"$buildRoot\dex",'classes.dex')
Invoke-Checked "$BuildTools\zipalign.exe" @('-p','-f','4',"$buildRoot\unsigned.apk","$buildRoot\aligned.apk")
if(!(Test-Path -LiteralPath $Keystore)){Invoke-Checked "$Jdk\bin\keytool.exe" @('-genkeypair','-keystore',$Keystore,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Meu Onibus Development, O=Development, C=BR','-keyalg','RSA','-keysize','2048','-validity','10000')}
Invoke-Checked "$Jdk\bin\java.exe" @('-jar',"$BuildTools\lib\apksigner.jar",'sign','--ks',$Keystore,'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',"$projectRoot\Meu-Onibus.apk","$buildRoot\aligned.apk")
Invoke-Checked "$Jdk\bin\java.exe" @('-jar',"$BuildTools\lib\apksigner.jar",'verify','--verbose',"$projectRoot\Meu-Onibus.apk")
Write-Output "APK: $projectRoot\Meu-Onibus.apk"
