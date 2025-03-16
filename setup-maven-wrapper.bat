@echo off
echo Starting Maven Wrapper generation...
echo.

mvn -N io.takari:maven:wrapper

echo.
echo If successful, you should see mvnw.cmd in your directory.
echo.

dir mvnw* /b 2>nul || echo No wrapper files found.

pause