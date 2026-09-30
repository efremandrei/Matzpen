$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$secretsRoot = Join-Path $env:USERPROFILE '.android\keystores'
$keystorePath = Join-Path $secretsRoot 'matzpen-release.jks'
$passwordPath = Join-Path $secretsRoot 'matzpen-release-password.dpapi'
$keytoolPath = 'C:\Program Files\Java\jdk-21\bin\keytool.exe'

New-Item -ItemType Directory -Path $secretsRoot -Force | Out-Null
if (-not (Test-Path -LiteralPath $passwordPath)) {
    $plain = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(36))
    $secure = ConvertTo-SecureString -String $plain -AsPlainText -Force
    ConvertFrom-SecureString -SecureString $secure | Set-Content -LiteralPath $passwordPath
    $plain = $null
}
$secure = Get-Content -LiteralPath $passwordPath | ConvertTo-SecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }

$env:MATZPEN_KEYSTORE = $keystorePath
$env:MATZPEN_STORE_PASSWORD = $password
$env:MATZPEN_KEY_PASSWORD = $password
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'

try {
    if (-not (Test-Path -LiteralPath $keystorePath)) {
        & $keytoolPath -genkeypair -alias matzpen -keyalg RSA -keysize 3072 -validity 10000 -keystore $keystorePath -storetype PKCS12 -storepass:env MATZPEN_STORE_PASSWORD -keypass:env MATZPEN_KEY_PASSWORD -dname 'CN=Andrei Efremuahkin, OU=Matzpen, O=Personal, C=IL'
        if ($LASTEXITCODE -ne 0) { throw 'Could not create release keystore' }
    }
    Push-Location $projectRoot
    try {
        & .\gradlew.bat assembleRelease testDebugUnitTest --no-daemon
        if ($LASTEXITCODE -ne 0) { throw 'Release build failed' }
    } finally { Pop-Location }
    $apk = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk'
    $apksigner = Join-Path $env:ANDROID_HOME 'build-tools\35.0.0\apksigner.bat'
    & $apksigner verify --verbose --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
    $dist = Join-Path $projectRoot 'dist'
    New-Item -ItemType Directory -Path $dist -Force | Out-Null
    Copy-Item -LiteralPath $apk -Destination (Join-Path $dist 'Matzpen-v1.0.0.apk') -Force
    Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $dist 'Matzpen-v1.0.0.apk') | Select-Object Path,Hash
} finally {
    Remove-Item Env:MATZPEN_KEYSTORE,Env:MATZPEN_STORE_PASSWORD,Env:MATZPEN_KEY_PASSWORD -ErrorAction SilentlyContinue
    $password = $null
}
