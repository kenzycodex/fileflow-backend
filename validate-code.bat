@echo off
:: Simple FileFlow Code Validation Script (Windows)
setlocal enabledelayedexpansion

echo === Simple FileFlow Code Validation ===

:: Create logs directory if it doesn't exist
if not exist "logs" mkdir logs
for /f "tokens=2 delims==" %%a in ('wmic os get localdatetime /value') do set "datetime=%%a"
set LOG_FILE=logs\validation-%datetime:~0,8%-%datetime:~8,6%.log

:: Compile the code
echo. >> "%LOG_FILE%"
echo --- Compiling Code --- >> "%LOG_FILE%"
call mvnw.cmd clean compile -Dmaven.test.skip=true >> "%LOG_FILE%" 2>&1
if %errorlevel% equ 0 (
    echo ✅ Compilation successful. >> "%LOG_FILE%"
    echo ✅ Compilation successful.
) else (
    echo ❌ Compilation failed. Check %LOG_FILE% for errors. >> "%LOG_FILE%"
    echo ❌ Compilation failed. Check %LOG_FILE% for errors.
    exit /b 1
)

:: Run basic checks
echo. >> "%LOG_FILE%"
echo --- Running Basic Checks --- >> "%LOG_FILE%"
set ISSUES=0

:: Check for TODOs
findstr /s /n "TODO" src\main\* >> "%LOG_FILE%" 2>nul
if %errorlevel% equ 0 (
    echo ⚠️ Found TODO comments that need addressing >> "%LOG_FILE%"
    echo ⚠️ Found TODO comments that need addressing
    set ISSUES=1
)

:: Check for FIXMEs
findstr /s /n "FIXME" src\main\* >> "%LOG_FILE%" 2>nul
if %errorlevel% equ 0 (
    echo ⚠️ Found FIXME comments that need attention >> "%LOG_FILE%"
    echo ⚠️ Found FIXME comments that need attention
    set ISSUES=1
)

:: Check for System.out.println
findstr /s /n "System.out.println" src\main\* >> "%LOG_FILE%" 2>nul
if %errorlevel% equ 0 (
    echo ⚠️ Found debug print statements >> "%LOG_FILE%"
    echo ⚠️ Found debug print statements
    set ISSUES=1
)

:: Run tests
echo. >> "%LOG_FILE%"
echo --- Running Tests --- >> "%LOG_FILE%"
call mvnw.cmd test >> "%LOG_FILE%" 2>&1
if %errorlevel% equ 0 (
    echo ✅ All tests passed. >> "%LOG_FILE%"
    echo ✅ All tests passed.
) else (
    echo ❌ Some tests failed. >> "%LOG_FILE%"
    echo ❌ Some tests failed.
    set ISSUES=1
)

:: Summary
echo. >> "%LOG_FILE%"
echo === Validation Summary === >> "%LOG_FILE%"
if %ISSUES% equ 0 (
    echo ✅ No major issues detected! >> "%LOG_FILE%"
    echo ✅ No major issues detected!
) else (
    echo ⚠️ Some issues were found. Check the output above. >> "%LOG_FILE%"
    echo ⚠️ Some issues were found. Check the output above.
)

echo Detailed log saved to: %LOG_FILE% >> "%LOG_FILE%"
echo Detailed log saved to: %LOG_FILE%
exit /b %ISSUES%