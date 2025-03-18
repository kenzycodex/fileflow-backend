#!/bin/bash

# FileFlow Development Environment Setup Script
# This script sets up a development environment for FileFlow

# Exit on error
set -e

echo "=== FileFlow Development Environment Setup ==="
echo "This script will set up a development environment for FileFlow"

# Check for Docker and Docker Compose
command -v docker >/dev/null 2>&1 || { echo >&2 "Docker is required but not installed. Aborting."; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo >&2 "Docker Compose is required but not installed. Aborting."; exit 1; }

# Check for Java
command -v java >/dev/null 2>&1 || { echo >&2 "Java is required but not installed. Please install JDK 17 or later. Aborting."; exit 1; }

# Check for Maven
command -v mvn >/dev/null 2>&1 || { echo >&2 "Maven is recommended but not installed. We'll use the Maven wrapper instead."; }

echo "Starting development dependencies (MySQL, MinIO)..."
docker-compose -f docker-compose-dev.yml up -d

echo "Waiting for MySQL to start..."
sleep 10

echo "Building the application..."
./mvnw clean package -DskipTests

echo "Starting the application in development mode..."
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

echo "=== Setup Complete ==="
echo "FileFlow is now running in development mode"
echo "API documentation: http://localhost:8080/swagger-ui.html"
echo "MinIO Console: http://localhost:9001 (minioadmin/minioadmin)"
echo "To stop the application, press Ctrl+C"
echo "To stop the development dependencies, run: docker-compose -f docker-compose-dev.yml down"