Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "Setting up Maven Wrapper" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

# Check Maven installation
try {
    $mvnVersion = mvn -version
    Write-Host "Maven found:" -ForegroundColor Green
    Write-Host $mvnVersion
}
catch {
    Write-Host "ERROR: Maven not found in PATH or not working properly." -ForegroundColor Red
    Write-Host "Please install Maven and ensure it's in your PATH."
    Read-Host "Press Enter to exit"
    exit 1
}

# Generate Maven Wrapper
Write-Host ""
Write-Host "Generating Maven Wrapper..." -ForegroundColor Yellow
try {
    mvn -N io.takari:maven:wrapper

    # Check if wrapper was created
    if (Test-Path "mvnw.cmd") {
        Write-Host ""
        Write-Host "SUCCESS: Maven Wrapper created successfully!" -ForegroundColor Green
        Write-Host "You can now use ./mvnw or mvnw.cmd instead of 'mvn'"

        # List wrapper files
        Write-Host ""
        Write-Host "Wrapper files created:" -ForegroundColor Cyan
        Get-ChildItem -Path . -Filter "mvnw*" | ForEach-Object { Write-Host "- $($_.Name)" }
        Get-ChildItem -Path ".mvn" -Recurse | ForEach-Object { Write-Host "- .mvn/$($_.FullName.Substring($pwd.Path.Length + 6))" }
    }
    else {
        Write-Host "Warning: mvnw.cmd not found after wrapper generation." -ForegroundColor Yellow
        Write-Host "Trying alternative method..." -ForegroundColor Yellow

        # Try alternative method
        mvn wrapper:wrapper

        if (Test-Path "mvnw.cmd") {
            Write-Host "SUCCESS: Maven Wrapper created using alternative method!" -ForegroundColor Green
        }
        else {
            Write-Host "ERROR: Failed to generate Maven Wrapper." -ForegroundColor Red
        }
    }
}
catch {
    Write-Host "ERROR: Failed to generate Maven Wrapper." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Read-Host "Press Enter to exit"