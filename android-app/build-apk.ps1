$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "D:\Android\jdk-17"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:PATH = "$env:JAVA_HOME\bin;D:\Android\gradle-8.5\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " [Jev Assistant] 正在构建 Android Debug APK..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$CurrentDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& "D:\Android\gradle-8.5\bin\gradle.bat" -p $CurrentDir assembleDebug

$ApkPath = "$CurrentDir\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $ApkPath) {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Green
    Write-Host " APK 构建完成！" -ForegroundColor Green
    Write-Host " 路径: $ApkPath" -ForegroundColor Green
    Write-Host " 大小: $([math]::Round((Get-Item $ApkPath).Length / 1MB, 2)) MB" -ForegroundColor Green
    Write-Host "==========================================" -ForegroundColor Green
} else {
    Write-Host "未找到生成的 APK。" -ForegroundColor Red
}
