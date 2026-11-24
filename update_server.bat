@echo off
REM For the administrator: closes the running BSK programs (after asking), updates, then starts the server.
REM To go back to the previous version: update_server.bat rollback
cd /d "%~dp0"
if /i "%~1"=="rollback" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bsk_update.ps1" -App server -Mode update -Rollback
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bsk_update.ps1" -App server -Mode update
)
echo.
pause
