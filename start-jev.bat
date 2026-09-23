@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -Command "try { Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8766/api/state -TimeoutSec 2 | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
if errorlevel 1 start "JEV Home Assistant Server" /min py -3 jev_server.py
timeout /t 2 /nobreak >nul
start "" http://127.0.0.1:8766/
endlocal
