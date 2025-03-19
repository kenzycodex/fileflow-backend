#!/bin/bash

# FileFlow Code Validation Script
# This script validates the code without running the full application

# Print header
echo "=== FileFlow Code Validation ==="
echo "Running code quality checks and compilation..."

# Compile the code
echo -e "\n--- Compiling Code ---"
./mvnw clean compile
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed. Please fix the above errors."
    exit 1
else
    echo "✅ Compilation successful."
fi

# Run SpotBugs
echo -e "\n--- Running SpotBugs (Bug Detection) ---"
./mvnw spotbugs:check
if [ $? -ne 0 ]; then
    echo "⚠️ SpotBugs found potential bugs. Review the report above."
else
    echo "✅ SpotBugs check passed."
fi

# Run PMD
echo -e "\n--- Running PMD (Code Quality) ---"
./mvnw pmd:check
if [ $? -ne 0 ]; then
    echo "⚠️ PMD found code quality issues. Review the report above."
else
    echo "✅ PMD check passed."
fi

# Run Checkstyle
echo -e "\n--- Running Checkstyle (Coding Standards) ---"
./mvnw checkstyle:check
if [ $? -ne 0 ]; then
    echo "⚠️ Checkstyle found coding standard issues. Review the report above."
else
    echo "✅ Checkstyle check passed."
fi

# Analyze dependencies
echo -e "\n--- Analyzing Dependencies ---"
./mvnw dependency:analyze
if [ $? -ne 0 ]; then
    echo "⚠️ Dependency analysis found issues. Review the report above."
else
    echo "✅ Dependency analysis passed."
fi

# Run unit tests
echo -e "\n--- Running Unit Tests ---"
./mvnw test
if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Please fix the failing tests."
else
    echo "✅ All tests passed."
fi

echo -e "\n=== Validation Complete ==="
echo "Review any warnings or errors and fix them before running the application."