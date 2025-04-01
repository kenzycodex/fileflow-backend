@echo off
:: FileFlow Development Environment Setup Script
:: Windows batch version of setup-dev.sh

:: Exit on error
setlocal enabledelayedexpansion

:: Set default values
set VALIDATE_ONLY=false

:: Process command line arguments
if "%1"=="--validate-only" set VALIDATE_ONLY=true
if "%1"=="--help" goto show_help
if not "%1"=="" if not "%1"=="--validate-only" goto show_help

echo === FileFlow Development Environment Setup ===
echo This script will set up a development environment for FileFlow

:: Check for Docker and Docker Compose
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo Docker is required but not installed. Aborting.
    exit /b 1
)

where docker-compose >nul 2>&1
if %errorlevel% neq 0 (
    echo Docker Compose is required but not installed. Aborting.
    exit /b 1
)

:: Check for Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo Java is required but not installed. Please install JDK 17 or later. Aborting.
    exit /b 1
)

:: Check for Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo Maven is recommended but not installed. We'll use the Maven wrapper instead.
)

if "%VALIDATE_ONLY%"=="true" (
    echo Running code validation only...
    call validate-code.bat
    exit /b %errorlevel%
)

echo Starting development dependencies (MySQL, MinIO)...
docker-compose -f docker-compose-dev.yml down
docker-compose -f docker-compose-dev.yml up -d

echo Waiting for MySQL to start...
timeout /t 10 >nul

echo Building the application...
call mvnw.cmd clean package -DskipTests

echo Running code validation...
call validate-code.bat
if %errorlevel% neq 0 (
    echo ⚠️ Code validation found issues. You can still proceed, but consider fixing them.
    set /p CONTINUE=Do you want to continue starting the application? (y/n):
    if /i not "!CONTINUE!"=="y" (
        echo Exiting. Fix the issues and try again.
        exit /b 1
    )
)

echo Starting the application in development mode...
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

echo === Setup Complete ===
echo FileFlow is now running in development mode
echo API documentation: http://localhost:8080/swagger-ui.html
echo MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
echo To stop the application, press Ctrl+C
echo To stop the development dependencies, run: docker-compose -f docker-compose-dev.yml down

goto :eof

:show_help
echo Usage: %~n0 [OPTIONS]
echo Options:
echo   --validate-only   Validate code without starting the application
echo   --help            Show this help message
echo.
exit /b 0