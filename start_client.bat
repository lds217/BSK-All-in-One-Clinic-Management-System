@echo off
REM Daily launcher for the BSK client: updates if a new version is available, then starts it.
REM Point the desktop shortcut at this file.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bsk_update.ps1" -App client -Mode start
if errorlevel 3 goto fallback
if errorlevel 2 goto end
if errorlevel 1 goto fallback
goto end

:fallback
REM The update script itself failed: start the installed version anyway.
start "" "%~dp0client.jar"

:end
