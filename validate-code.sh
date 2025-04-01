#!/bin/bash

# Simple FileFlow Code Validation Script

echo "=== Simple FileFlow Code Validation ==="

# Create logs directory if it doesn't exist
mkdir -p logs
LOG_FILE="logs/validation-$(date +%Y%m%d-%H%M%S).log"

# Function to log messages
log() {
  echo "$1" | tee -a "$LOG_FILE"
}

# Compile the code
log "\n--- Compiling Code ---"
if ./mvnw clean compile -Dmaven.test.skip=true; then
    log "✅ Compilation successful."
else
    log "❌ Compilation failed. Check $LOG_FILE for errors."
    exit 1
fi

# Run basic checks
log "\n--- Running Basic Checks ---"
ISSUES=0

# Check for TODOs
if grep -r "TODO" src/main/; then
    log "⚠️ Found TODO comments that need addressing"
    ISSUES=1
fi

# Check for FIXMEs
if grep -r "FIXME" src/main/; then
    log "⚠️ Found FIXME comments that need attention"
    ISSUES=1
fi

# Check for System.out.println
if grep -r "System.out.println" src/main/; then
    log "⚠️ Found debug print statements"
    ISSUES=1
fi

# Run tests
log "\n--- Running Tests ---"
if ./mvnw test; then
    log "✅ All tests passed."
else
    log "❌ Some tests failed."
    ISSUES=1
fi

# Summary
log "\n=== Validation Summary ==="
if [ $ISSUES -eq 0 ]; then
    log "✅ No major issues detected!"
else
    log "⚠️ Some issues were found. Check the output above."
fi

log "Detailed log saved to: $LOG_FILE"
exit $ISSUES