@echo off
setlocal enabledelayedexpansion

:: Navigate to script directory
cd /d "%~dp0"

echo ========================================================
echo    Currency Database Dashboard Console Launcher
echo ========================================================
echo.

call "%~dp0run.bat" dashboard %*
