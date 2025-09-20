@echo off
REM BSK Server Update Script Launcher
REM This batch file runs the PowerShell update script with proper execution policy

echo BSK Server Update Launcher
echo ========================
echo.

REM Change to the script directory
cd /d "%~dp0"

REM Run the PowerShell script with execution policy bypass
powershell -ExecutionPolicy Bypass -File ".\update_server_script.ps1"

REM Keep the window open if there's an error
if errorlevel 1 (
    echo.
    echo An error occurred. Press any key to exit...
    pause >nul
)
