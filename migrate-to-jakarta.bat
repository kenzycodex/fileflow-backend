@echo off
setlocal EnableDelayedExpansion

echo ======================================================
echo Jakarta EE Migration Tool - Converting javax to jakarta
echo ======================================================
echo.

:: Set the directory to scan - default is the current directory
set "SOURCE_DIR=%~dp0src"
echo Scanning directory: %SOURCE_DIR%
echo.

:: Create a backup folder
set "BACKUP_DIR=%~dp0backup_%date:~-4,4%%date:~-7,2%%date:~-10,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
set "BACKUP_DIR=%BACKUP_DIR: =0%"
echo Creating backup in: %BACKUP_DIR%
mkdir "%BACKUP_DIR%"

:: Counter for modified files
set "MODIFIED_COUNT=0"

:: Find all .java files recursively
for /r "%SOURCE_DIR%" %%f in (*.java) do (
    set "FILE=%%f"
    set "RELATIVE_PATH=%%~pf"
    set "RELATIVE_PATH=!RELATIVE_PATH:%SOURCE_DIR%=!"
    set "FILENAME=%%~nxf"

    :: Create backup directory structure
    if not exist "%BACKUP_DIR%!RELATIVE_PATH!" mkdir "%BACKUP_DIR%!RELATIVE_PATH!"

    :: Copy original file to backup
    copy "!FILE!" "%BACKUP_DIR%!RELATIVE_PATH!!FILENAME!" > nul

    :: Check if file contains javax patterns
    findstr /i /m "javax\.persistence javax\.validation javax\.servlet" "!FILE!" > nul
    if !errorlevel! equ 0 (
        echo Processing: !FILENAME!

        :: Create a temporary file
        set "TEMP_FILE=%TEMP%\jakarta_temp_%RANDOM%.java"

        :: Process the file more safely
        powershell -Command "(Get-Content '!FILE!' -Raw) -replace 'javax\.persistence', 'jakarta.persistence' -replace 'javax\.validation', 'jakarta.validation' -replace 'javax\.servlet', 'jakarta.servlet' | Set-Content -Path '!TEMP_FILE!'"

        :: Check if temp file was created and has content
        if exist "!TEMP_FILE!" (
            for %%s in ("!TEMP_FILE!") do set TEMP_SIZE=%%~zs
            if !TEMP_SIZE! gtr 0 (
                :: Replace the original file with the modified one
                copy /y "!TEMP_FILE!" "!FILE!" > nul
                set /a "MODIFIED_COUNT+=1"
            ) else (
                echo WARNING: Temp file for !FILENAME! was empty. Skipping this file.
            )
            del "!TEMP_FILE!" > nul
        ) else (
            echo WARNING: Failed to create temp file for !FILENAME!
        )
    )
)

echo.
echo ======================================================
echo Migration complete! Modified %MODIFIED_COUNT% files.
echo Backup created in: %BACKUP_DIR%
echo.
echo Remember to:
echo 1. Update your SecurityConfig to the new Spring Security 6 approach
echo 2. Check for missing classes and create them
echo 3. Run a clean build with 'mvnw clean package'
echo ======================================================
echo.

pause