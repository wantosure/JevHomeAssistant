@echo off
set JAVA_HOME=D:\Android\jdk-17
set ANDROID_HOME=D:\Android\sdk
set PATH=%JAVA_HOME%\bin;D:\Android\gradle-8.5\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo [Jev Assistant] 开始编译 Android APK...
cd /d "%~dp0"
call "D:\Android\gradle-8.5\bin\gradle.bat" assembleDebug --no-daemon

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo  编译成功！APK 输出位置：
    echo  app\build\outputs\apk\debug\app-debug.apk
    echo ========================================================
) else (
    echo 编译出错，请检查日志。
)
pause
