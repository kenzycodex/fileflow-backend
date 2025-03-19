#!/bin/bash

# FileFlow Development Environment Setup Script
# This script sets up a development environment for FileFlow

# Exit on error
set -e

show_help() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  --validate-only   Validate code without starting the application"
    echo "  --help            Show this help message"
    echo ""
}

# Process command line arguments
VALIDATE_ONLY=false
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --validate-only) VALIDATE_ONLY=true ;;
        --help) show_help; exit 0 ;;
        *) echo "Unknown parameter: $1"; show_help; exit 1 ;;
    esac
    shift
done

echo "=== FileFlow Development Environment Setup ==="
echo "This script will set up a development environment for FileFlow"

# Check for Docker and Docker Compose
command -v docker >/dev/null 2>&1 || { echo >&2 "Docker is required but not installed. Aborting."; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo >&2 "Docker Compose is required but not installed. Aborting."; exit 1; }

# Check for Java
command -v java >/dev/null 2>&1 || { echo >&2 "Java is required but not installed. Please install JDK 17 or later. Aborting."; exit 1; }

# Check for Maven
command -v mvn >/dev/null 2>&1 || { echo >&2 "Maven is recommended but not installed. We'll use the Maven wrapper instead."; }

if [ "$VALIDATE_ONLY" = true ]; then
    echo "Running code validation only..."
    bash ./validate-code.sh
    exit $?
fi

echo "Starting development dependencies (MySQL, MinIO)..."
docker-compose -f docker-compose-dev.yml up -d

echo "Waiting for MySQL to start..."
sleep 10

echo "Building the application..."
./mvnw clean package -DskipTests

echo "Running code validation..."
bash ./validate-code.sh
if [ $? -ne 0 ]; then
    echo "⚠️ Code validation found issues. You can still proceed, but consider fixing them."
    read -p "Do you want to continue starting the application? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Exiting. Fix the issues and try again."
        exit 1
    fi
fi

echo "Starting the application in development mode..."
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

echo "=== Setup Complete ==="
echo "FileFlow is now running in development mode"
echo "API documentation: http://localhost:8080/swagger-ui.html"
echo "MinIO Console: http://localhost:9001 (minioadmin/minioadmin)"
echo "To stop the application, press Ctrl+C"
echo "To stop the development dependencies, run: docker-compose -f docker-compose-dev.yml down"