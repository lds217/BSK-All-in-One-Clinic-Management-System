@echo off
REM Launcher for the BSK server: updates if a new version is available, then starts it.
REM Point the desktop shortcut (and, if wanted, a shortcut in shell:startup) at this file.
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bsk_update.ps1" -App server -Mode start
if errorlevel 3 goto fallback
if errorlevel 2 goto running
if errorlevel 1 goto fallback
goto end

:running
REM The server is already running.
timeout /t 5 >nul
goto end

:fallback
REM The update script itself failed: start the installed version anyway.
start "" "%~dp0server.jar"

:end
