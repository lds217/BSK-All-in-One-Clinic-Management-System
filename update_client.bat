@echo off
REM For the administrator: closes the running BSK programs (after asking), updates, then starts the client.
REM To go back to the previous version: update_client.bat rollback
cd /d "%~dp0"
if /i "%~1"=="rollback" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bsk_update.ps1" -App client -Mode update -Rollback
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bsk_update.ps1" -App client -Mode update
)
echo.
pause
