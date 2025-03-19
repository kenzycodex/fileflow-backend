# FileFlow Setup Guide

This guide provides detailed instructions for setting up and running the FileFlow application in different environments.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Development Environment](#development-environment)
- [Code Validation](#code-validation)
- [Production Environment](#production-environment)
- [Optional Components](#optional-components)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Before setting up FileFlow, ensure you have the following installed:

- Java 17 or later
- Maven 3.6.3 or later (or use the included Maven wrapper)
- Docker and Docker Compose (for containerized deployment)
- Git (for cloning the repository)

## Development Environment

### Option 1: Using Docker Compose for Dependencies

This is the recommended approach for development, as it runs only the dependencies (MySQL, MinIO, Elasticsearch) in Docker, while the application runs locally for easier debugging.

1. Clone the repository:
   ```bash
   git clone https://github.com/kenzycodex/fileflow-backend.git
   cd fileflow-backend
   ```

2. Start the dependencies:
   ```bash
   docker-compose -f docker-compose-dev.yml up -d
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   For development with Elasticsearch enabled:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,elasticsearch
   ```

4. The application will be available at: http://localhost:8080

### Option 2: Using the Setup Script

For even easier setup, use the provided setup script:

```bash
chmod +x setup-dev.sh
./setup-dev.sh
```

To validate code without starting the application:
```bash
./setup-dev.sh --validate-only
```

### Option 3: Manual Setup

If you prefer not to use Docker, you can set up the dependencies manually:

1. Install and configure MySQL 8.0
   - Create a database named `fileflow`
   - Update `application-dev.properties` with your database credentials

2. Configure local storage:
   - In `application-dev.properties`, set `app.storage.strategy=local`
   - Ensure the application has write access to the storage directory

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## Code Validation

FileFlow includes tools to identify code issues early without running the full application:

### Using the Validation Script

The `validate-code.sh` script runs a sequence of checks to catch issues in the codebase:

```bash
chmod +x validate-code.sh
./validate-code.sh
```

This will run:
- Compilation checks
- SpotBugs for bug detection
- PMD for code quality analysis
- Checkstyle for coding standards
- Dependency analysis
- Unit tests

### Individual Validation Commands

You can also run specific validation steps:

1. Compile the project:
   ```bash
   ./mvnw clean compile
   ```

2. Run static analysis tools:
   ```bash
   ./mvnw spotbugs:check    # Bug detection
   ./mvnw pmd:check         # Code quality
   ./mvnw checkstyle:check  # Coding standards
   ```

3. Analyze dependencies:
   ```bash
   ./mvnw dependency:analyze
   ```

4. Run unit tests:
   ```bash
   ./mvnw test
   ```

## Production Environment

### Option 1: Docker Compose Deployment

1. Clone the repository:
   ```bash
   git clone https://github.com/kenzycodex/fileflow-backend.git
   cd fileflow-backend
   ```

2. Build and start the containers:
   ```bash
   docker-compose up -d
   ```

   This will start all required services, including the application, MySQL, MinIO, and Elasticsearch.

3. The application will be available at: http://localhost:8080

### Option 2: Kubernetes Deployment

For production Kubernetes deployment, refer to the Kubernetes deployment guide in `k8s/README.md`.

### SSL Configuration

For production, it's strongly recommended to configure SSL:

1. Obtain SSL certificates for your domain
2. Configure SSL in `application-prod.properties`:
   ```properties
   server.ssl.enabled=true
   server.ssl.key-store=/path/to/keystore.p12
   server.ssl.key-store-password=your-password
   server.ssl.key-store-type=PKCS12
   server.ssl.key-alias=your-key-alias
   ```

## Optional Components

### Elasticsearch Setup

Elasticsearch is optional but recommended for full-text search capabilities:

1. Start Elasticsearch:
   ```bash
   docker-compose -f docker-compose-dev.yml up -d elasticsearch
   ```

2. Enable Elasticsearch in the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,elasticsearch
   ```

3. To verify Elasticsearch is working, check:
   ```bash
   curl http://localhost:9200
   ```

### MinIO Setup

MinIO is recommended for production storage:

1. Start MinIO:
   ```bash
   docker-compose -f docker-compose-dev.yml up -d minio
   ```

2. Access the MinIO console at: http://localhost:9001
   - Username: minioadmin
   - Password: minioadmin

3. Create a bucket named `fileflow` if not already created

4. Configure the application to use MinIO:
   ```properties
   app.storage.strategy=minio
   app.minio.endpoint=http://localhost:9000
   app.minio.access-key=minioadmin
   app.minio.secret-key=minioadmin
   app.minio.bucket=fileflow
   ```

## Environment Configuration

### Development

The development profile is configured in `application-dev.properties`. Key settings include:

- Database connection details
- Logging levels (more verbose)
- Storage configuration (local by default)
- Disabled mail sending

### Production

The production profile is configured in `application-prod.properties`. Key settings include:

- Database connection via environment variables
- Optimized logging levels
- MinIO storage configuration
- SSL configuration
- Redis caching

### Environment Variables

In production, sensitive information should be provided via environment variables:

- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `APP_MINIO_ENDPOINT` - MinIO endpoint URL
- `APP_MINIO_ACCESS_KEY` - MinIO access key
- `APP_MINIO_SECRET_KEY` - MinIO secret key
- `APP_MINIO_BUCKET` - MinIO bucket name

## Troubleshooting

### Database Connection Issues

- Ensure MySQL is running: `docker ps | grep mysql`
- Check the database connection parameters in `application.properties`
- Verify that the database and user exist: `mysql -u root -p -e "SHOW DATABASES;"`

### MinIO Connection Issues

- Ensure MinIO is running: `docker ps | grep minio`
- Verify MinIO credentials
- Check if the bucket exists: http://localhost:9001/browser/fileflow

### Elasticsearch Issues

- Ensure Elasticsearch is running: `docker ps | grep elasticsearch`
- Check the connection: `curl http://localhost:9200`
- Verify the indices: `curl http://localhost:9200/_cat/indices`

### Application Startup Issues

- Check the application logs: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- Verify environment variables are set correctly
- Check file permissions for storage directories
- Run the validation script to detect issues: `./validate-code.sh`