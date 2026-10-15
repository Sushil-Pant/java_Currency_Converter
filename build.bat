@echo off
setlocal

:: Navigate to script directory
cd /d "%~dp0"

echo ========================================================
echo    Currency Converter - Project Build and Compile
echo ========================================================
echo.

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] 'javac' compiler was not found in your PATH.
    echo Please ensure the Java Development Kit is installed
    echo and the JDK bin folder is added to your system PATH.
    echo.
    pause
    exit /b 1
)

set "CP=.;lib/*"

echo [INFO] Compiling all Java source files with classpath "%CP%"...
javac -cp "%CP%" *.java

if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

echo.
echo [OK] Build successful. All class files generated.
echo.
pause
exit /b 0
