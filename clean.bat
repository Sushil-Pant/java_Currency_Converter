@echo off
setlocal

cd /d "%~dp0"

echo [INFO] Cleaning compiled .class files...
del /q *.class 2>nul
echo [OK] Clean complete.
pause
