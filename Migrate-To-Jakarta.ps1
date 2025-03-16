# Jakarta EE Migration Tool - PowerShell Edition
Write-Host "======================================================"
Write-Host "Jakarta EE Migration Tool - Converting javax to jakarta"
Write-Host "======================================================"
Write-Host ""

# Set the directory to scan - default is the script directory
$SOURCE_DIR = Join-Path $PSScriptRoot "src"
Write-Host "Scanning directory: $SOURCE_DIR"
Write-Host ""

# Create a backup folder
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_DIR = Join-Path $PSScriptRoot "backup_$timestamp"
Write-Host "Creating backup in: $BACKUP_DIR"
New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null

# Counter for modified files
$MODIFIED_COUNT = 0

# Find all .java files recursively
$javaFiles = Get-ChildItem -Path $SOURCE_DIR -Filter *.java -Recurse

foreach ($file in $javaFiles) {
    $relativePath = $file.DirectoryName.Substring($SOURCE_DIR.Length)
    $backupPath = Join-Path $BACKUP_DIR $relativePath

    # Create backup directory structure
    if (-not (Test-Path $backupPath)) {
        New-Item -ItemType Directory -Path $backupPath -Force | Out-Null
    }

    # Copy original file to backup
    Copy-Item -Path $file.FullName -Destination (Join-Path $backupPath $file.Name) -Force

    # Check if file contains javax patterns
    $content = Get-Content -Path $file.FullName -Raw
    if ($content -match "javax\.persistence|javax\.validation|javax\.servlet") {
        Write-Host "Processing: $($file.Name)"

        # Replace imports
        $newContent = $content -replace "javax\.persistence", "jakarta.persistence" `
                              -replace "javax\.validation", "jakarta.validation" `
                              -replace "javax\.servlet", "jakarta.servlet"

        # Save the file
        [System.IO.File]::WriteAllText($file.FullName, $newContent)
        $MODIFIED_COUNT++
    }
}

Write-Host ""
Write-Host "======================================================"
Write-Host "Migration complete! Modified $MODIFIED_COUNT files."
Write-Host "Backup created in: $BACKUP_DIR"
Write-Host ""
Write-Host "Remember to:"
Write-Host "1. Update your SecurityConfig to the new Spring Security 6 approach"
Write-Host "2. Check for missing classes and create them"
Write-Host "3. Run a clean build with 'mvnw clean package'"
Write-Host "======================================================"
Write-Host ""

Read-Host -Prompt "Press Enter to continue"