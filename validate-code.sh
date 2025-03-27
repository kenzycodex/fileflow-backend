#!/bin/bash

# FileFlow Code Validation Script
# This script validates the code without running the full application

# Create logs directory if it doesn't exist
mkdir -p logs

# Log file
LOG_FILE="logs/validation-$(date +%Y%m%d-%H%M%S).log"

# Function to log messages to both console and file
log() {
  echo -e "$1" | tee -a "$LOG_FILE"
}

# Print header
log "=== FileFlow Code Validation ==="
log "Running code quality checks and compilation..."
log "Detailed results will be saved to: $LOG_FILE"

# Compile the code
log "\n--- Compiling Code ---"
./mvnw clean compile -Dmaven.test.skip=true | tee -a "$LOG_FILE"
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    log "❌ Compilation failed. Errors found in the following files:"
    # Extract file paths from the compilation errors
    grep -B 1 -A 2 "\[ERROR\]" "$LOG_FILE" | grep "\.java" | sed 's/.*\[\(.*\.java\).*\].*/\1/' | sort | uniq | while read -r file; do
        log "   - $file"
    done
    log "\nPlease fix these errors before continuing."
    log "For detailed error messages, check the log file: $LOG_FILE"
    exit 1
else
    log "✅ Compilation successful."
fi

# Find potential issues in Java files
log "\n--- Scanning for Common Java Code Issues ---"
ISSUES_FOUND=0

# Function to check for patterns in files and report issues
check_pattern() {
    local pattern="$1"
    local description="$2"
    local files=$(grep -l "$pattern" $(find src/main -name "*.java"))

    if [ -n "$files" ]; then
        ISSUES_FOUND=1
        log "\n⚠️ $description:"
        echo "$files" | while read -r file; do
            log "   - $file"
            grep -n "$pattern" "$file" | sed 's/^/     Line /'
        done
    fi
}

# Check for common issues
check_pattern "System\.out\.println" "Found debug print statements"
check_pattern "TODO" "Found TODO comments that need to be addressed"
check_pattern "FIXME" "Found FIXME comments that need attention"
check_pattern "@Autowired.*private" "Field injection found (consider constructor injection instead)"
check_pattern "throw new \(RuntimeException\|Exception\)" "Generic exceptions found (consider specific exceptions)"
check_pattern "null *==" "Potential null pointer issue (null should be on the right side)"
check_pattern "catch *(.*).*{ *}" "Empty catch blocks found"

# Check internet connectivity to Maven Central
log "\n--- Checking Internet Connectivity ---"
ping -c 1 repo.maven.apache.org > /dev/null 2>&1
INTERNET_AVAILABLE=$?

if [ $INTERNET_AVAILABLE -ne 0 ]; then
    log "⚠️ Limited internet connectivity detected. Skipping dependency downloads."
    log "⚠️ Some validation checks will be skipped."
else
    # Run SpotBugs
    log "\n--- Running SpotBugs (Bug Detection) ---"
    ./mvnw spotbugs:check -Dspotbugs.skip=false -Dspotbugs.failOnError=false -Dspotbugs.outputDirectory=target/spotbugs -Dspotbugs.effort=Max -Dspotbugs.xmlOutput=true | tee -a "$LOG_FILE"
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        log "⚠️ SpotBugs found potential bugs."
        # Create a human-readable report from XML
        if [ -f "target/spotbugs/spotbugsXml.xml" ]; then
            log "\nTop issues by type:"
            xmllint --xpath "//BugInstance/@type" target/spotbugs/spotbugsXml.xml 2>/dev/null | tr ' ' '\n' | sort | uniq -c | sort -nr | head -10 | while read -r count type; do
                type=$(echo $type | sed 's/type=//g' | sed 's/"//g')
                log "   - $count occurrences of $type"
            done

            log "\nFiles with most issues:"
            xmllint --xpath "//BugInstance/SourceLine/@sourcepath" target/spotbugs/spotbugsXml.xml 2>/dev/null | tr ' ' '\n' | sort | uniq -c | sort -nr | head -5 | while read -r count file; do
                file=$(echo $file | sed 's/sourcepath=//g' | sed 's/"//g')
                log "   - $file: $count issues"
            done

            log "\nView full report at: target/spotbugs/spotbugsXml.xml"
        fi
    else
        log "✅ SpotBugs check passed."
    fi

    # Run PMD
    log "\n--- Running PMD (Code Quality) ---"
    ./mvnw pmd:check -Dpmd.skip=false -Dpmd.failOnViolation=false | tee -a "$LOG_FILE"
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        log "⚠️ PMD found code quality issues."
        # Extract file paths from PMD output
        grep -A 3 "PMD.* Rule" "$LOG_FILE" | grep "\.java" | sort | uniq | head -10 | while read -r file; do
            log "   - $file"
        done
        log "\nSee log file for complete details: $LOG_FILE"
    else
        log "✅ PMD check passed."
    fi

    # Run Checkstyle
    log "\n--- Running Checkstyle (Coding Standards) ---"
    ./mvnw checkstyle:check -Dcheckstyle.skip=false -Dcheckstyle.failOnViolation=false | tee -a "$LOG_FILE"
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
        log "⚠️ Checkstyle found coding standard issues."
        # Extract files with issues
        grep "\[ERROR\] .*\.java" "$LOG_FILE" | sort | uniq | head -10 | while read -r line; do
            log "   - $line"
        done
        log "\nSee log file for complete details: $LOG_FILE"
    else
        log "✅ Checkstyle check passed."
    fi
fi

# Analyze dependencies
log "\n--- Analyzing Dependencies ---"
./mvnw dependency:analyze -DfailOnWarning=false | tee -a "$LOG_FILE"
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    log "⚠️ Dependency analysis found issues. Review the report above."
else
    # Extract and summarize dependency issues
    UNUSED=$(grep "Unused declared dependencies found" -A 20 "$LOG_FILE" | grep -v "WARNING" | grep -v "^\[" | grep -v "^$" | wc -l)
    UNDECLARED=$(grep "Used undeclared dependencies found" -A 20 "$LOG_FILE" | grep -v "WARNING" | grep -v "^\[" | grep -v "^$" | wc -l)

    if [ $UNUSED -gt 0 ] || [ $UNDECLARED -gt 0 ]; then
        log "⚠️ Dependency issues found:"
        if [ $UNUSED -gt 0 ]; then
            log "   - $UNUSED unused declared dependencies (bloating your project)"
        fi
        if [ $UNDECLARED -gt 0 ]; then
            log "   - $UNDECLARED used undeclared dependencies (potential build instability)"
        fi
        log "   Review the log file for details: $LOG_FILE"
    else
        log "✅ Dependency analysis passed."
    fi
fi

# Run unit tests
log "\n--- Running Unit Tests ---"
./mvnw test | tee -a "$LOG_FILE"
if [ ${PIPESTATUS[0]} -ne 0 ]; then
    log "❌ Tests failed. Failed tests:"
    grep "<<< FAILURE\!" "$LOG_FILE" -A 1 | grep -v "<<< FAILURE\!" | grep -v "\-\-" | while read -r test; do
        log "   - $test"
    done
    log "\nPlease fix the failing tests."
else
    log "✅ All tests passed."
fi

# Summary
log "\n=== Validation Summary ==="
if [ $ISSUES_FOUND -eq 1 ]; then
    log "⚠️ Basic code scan found issues that should be fixed."
fi

# Count total issues
TOTAL_ISSUES=$(grep -E "❌|⚠️" "$LOG_FILE" | wc -l)
if [ $TOTAL_ISSUES -gt 0 ]; then
    log "Found approximately $TOTAL_ISSUES issues to address."
    log "Review the log file for complete details: $LOG_FILE"
else
    log "✅ No major issues detected in the codebase!"
fi

log "\n=== Validation Complete ==="
log "Review any warnings or errors and fix them before running the application."