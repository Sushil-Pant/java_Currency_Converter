@echo off
setlocal

:: Navigate to script directory
cd /d "%~dp0"

echo ========================================================
echo    Currency Converter Application Launcher
echo ========================================================
echo.

:: 1. Verify Java Runtime
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] 'java' command was not found in your PATH.
    echo Please ensure Java runtime is installed and added to PATH.
    echo.
    pause
    exit /b 1
)

:: 2. Set Classpath (current directory + MySQL connector jar in lib)
set "CP=.;lib/*"

:: 3. Check for compiler and build if .class files are missing
if not exist "ProCurrencyConverter.class" (
    where javac >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] 'javac' compiler not found and no compiled class files exist.
        echo Please install JDK to compile the project.
        pause
        exit /b 1
    )
    echo [INFO] Compiled classes not found. Compiling Java sources...
    javac -cp "%CP%" *.java
    if errorlevel 1 (
        echo.
        echo [ERROR] Compilation failed.
        pause
        exit /b 1
    )
    echo [OK] Compilation completed successfully.
    echo.
)

:: 4. Determine target to run
set "TARGET_CLASS=ProCurrencyConverter"
if /i "%~1"=="dashboard" (
    set "TARGET_CLASS=DatabaseDashboardFrame"
) else if /i "%~1"=="db" (
    set "TARGET_CLASS=DatabaseDashboardFrame"
) else if /i "%~1"=="converter" (
    set "TARGET_CLASS=ProCurrencyConverter"
)

echo [INFO] Starting %TARGET_CLASS%...
echo.

java -cp "%CP%" %TARGET_CLASS% %*
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo [ERROR] Application terminated with exit code %EXIT_CODE%.
    pause
)

exit /b %EXIT_CODE%
