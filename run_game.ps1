param(
  [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$apkPath = Join-Path $projectRoot 'android\build\outputs\apk\debug\android-debug.apk'

if (!(Test-Path $AdbPath)) {
  throw "adb not found: $AdbPath"
}
if (!(Test-Path $apkPath)) {
  throw "APK not found: $apkPath. Build first: .\\gradlew.bat :android:assembleDebug"
}

& $AdbPath start-server | Out-Null
$devices = & $AdbPath devices
if ($devices -notmatch "\tdevice") {
  throw "No running emulator/device. Start AVD in Android Studio first."
}

& $AdbPath install -r $apkPath | Out-Host
& $AdbPath shell am start -n 'com.sao.aincrad.android/com.sao.aincrad.android.AndroidLauncher' | Out-Host
Write-Host "Game launched."
