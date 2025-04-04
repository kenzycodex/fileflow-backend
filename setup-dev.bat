@echo off
:: FileFlow Development Environment Setup Script
:: Windows batch version of setup-dev.sh

:: Exit on error
setlocal enabledelayedexpansion

:: Set default values
set VALIDATE_ONLY=false
set CLEAN_VOLUMES=false

:: Process command line arguments
if "%1"=="--validate-only" set VALIDATE_ONLY=true
if "%1"=="--clean" set CLEAN_VOLUMES=true
if "%1"=="--help" goto show_help
if not "%1"=="" if not "%1"=="--validate-only" if not "%1"=="--clean" goto show_help

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

echo Stopping existing development dependencies...
docker-compose -f docker-compose-dev.yml down
if %errorlevel% neq 0 (
    echo Failed to stop existing containers. Aborting.
    exit /b 1
)

:: Clean volumes if requested
if "%CLEAN_VOLUMES%"=="true" (
    echo Cleaning Docker volumes...
    for /f "tokens=*" %%v in ('docker volume ls --filter name^=fileflow -q') do (
        echo Removing volume: %%v
        docker volume rm %%v
        if !errorlevel! neq 0 (
            echo Warning: Failed to remove volume %%v
        )
    )
)

echo Starting development dependencies (MySQL, Redis, MinIO, Elasticsearch)...
docker-compose -f docker-compose-dev.yml up -d
if %errorlevel% neq 0 (
    echo Failed to start development dependencies. Aborting.
    exit /b 1
)

echo Waiting for MySQL to be ready...
set /a retry_count=0
set /a max_retries=30

:wait_for_mysql
set /a retry_count+=1
if %retry_count% gtr %max_retries% (
    echo MySQL did not become ready in time. Aborting.
    exit /b 1
)

docker-compose -f docker-compose-dev.yml exec -T mysql mysqladmin ping -h localhost -u fileflow -pfileflow --silent
if %errorlevel% neq 0 (
    echo Waiting for MySQL to be ready... Attempt %retry_count%/%max_retries%
    timeout /t 2 >nul
    goto wait_for_mysql
)

echo MySQL is ready!

echo Starting the application in development mode...
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
if %errorlevel% neq 0 (
    echo Failed to start the application.
    exit /b 1
)

goto end

:show_help
echo Usage: setup-dev.bat [OPTIONS]
echo Options:
echo   --validate-only  Validate the environment without starting services
echo   --clean          Clean up Docker volumes before starting (use when MySQL has corruption issues)
echo   --help           Show this help message
exit /b 0

:end
echo === Setup Complete ===
echo FileFlow is now running in development mode
echo API documentation: http://localhost:8080/swagger-ui.html
echo MinIO Console: http://localhost:9001 (minioadmin/minioadmin)
echo To stop the application, press Ctrl+C
echo To stop the development dependencies, run: docker-compose -f docker-compose-dev.yml down

exit /b 0