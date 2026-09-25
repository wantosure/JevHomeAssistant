$ErrorActionPreference = "Stop"

$AndroidDir = "D:\Android"
$SetupDir = "$AndroidDir\setup"
$JdkDir = "$AndroidDir\jdk-17"
$SdkDir = "$AndroidDir\sdk"

New-Item -ItemType Directory -Force -Path $SetupDir | Out-Null
New-Item -ItemType Directory -Force -Path $JdkDir | Out-Null
New-Item -ItemType Directory -Force -Path $SdkDir | Out-Null

# 1. Download & Extract JDK 17
$JdkZip = "$SetupDir\jdk-17.zip"
if (-not (Test-Path "$JdkDir\bin\java.exe")) {
    Write-Host "[1/4] Downloading OpenJDK 17..."
    $JdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip"
    curl.exe -L -o $JdkZip $JdkUrl
    Write-Host "[1/4] Extracting OpenJDK 17..."
    Expand-Archive -Path $JdkZip -DestinationPath "$SetupDir\jdk_temp" -Force
    $ExtractedJdk = Get-ChildItem "$SetupDir\jdk_temp" | Where-Object { $_.PSIsContainer } | Select-Object -First 1
    Move-Item -Path "$($ExtractedJdk.FullName)\*" -Destination $JdkDir -Force
    Remove-Item -Recurse -Force "$SetupDir\jdk_temp"
    Remove-Item -Force $JdkZip
}
Write-Host "Java is ready: $JdkDir\bin\java.exe"

# 2. Download & Extract Android Commandline Tools
$CmdToolsZip = "$SetupDir\commandlinetools.zip"
$CmdToolsTarget = "$SdkDir\cmdline-tools\latest"
if (-not (Test-Path "$CmdToolsTarget\bin\sdkmanager.bat")) {
    Write-Host "[2/4] Downloading Android Commandline Tools..."
    $CmdToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    curl.exe -L -o $CmdToolsZip $CmdToolsUrl
    Write-Host "[2/4] Extracting Commandline Tools..."
    Expand-Archive -Path $CmdToolsZip -DestinationPath "$SetupDir\cmdtools_temp" -Force
    New-Item -ItemType Directory -Force -Path "$SdkDir\cmdline-tools" | Out-Null
    Move-Item -Path "$SetupDir\cmdtools_temp\cmdline-tools" -Destination $CmdToolsTarget -Force
    Remove-Item -Recurse -Force "$SetupDir\cmdtools_temp"
    Remove-Item -Force $CmdToolsZip
}
Write-Host "sdkmanager is ready: $CmdToolsTarget\bin\sdkmanager.bat"

# 3. Accept licenses and install platform-tools, platforms;android-34, build-tools;34.0.0
$env:JAVA_HOME = $JdkDir
$env:PATH = "$JdkDir\bin;$CmdToolsTarget\bin;$env:PATH"
$env:ANDROID_HOME = $SdkDir

Write-Host "[3/4] Accepting Android SDK licenses..."
cmd.exe /c "echo y | `"$CmdToolsTarget\bin\sdkmanager.bat`" --sdk_root=`"$SdkDir`" --licenses"

Write-Host "[4/4] Installing platform-tools, platforms;android-34, build-tools;34.0.0..."
cmd.exe /c "echo y | `"$CmdToolsTarget\bin\sdkmanager.bat`" --sdk_root=`"$SdkDir`" `"platform-tools`" `"platforms;android-34`" `"build-tools;34.0.0`""

Write-Host "Android SDK Setup Complete!"
