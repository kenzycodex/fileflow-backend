# FileFlow Environment Setup Guide

This guide explains how to set up and configure the environment for FileFlow in both development and production scenarios.

## Overview of Environment Configuration

FileFlow uses a three-tier approach to environment configuration:

1. **System Environment Variables** (highest priority)
2. **.env File** (middle priority)
3. **application.properties** (lowest priority)

This setup allows for flexible configuration across different environments without hardcoding sensitive information.

## Development Setup

### 1. Create .env File

Create a `.env` file in your project root directory based on the `.env.example` template:

```bash
cp .env.example .env
```

Edit this file with your development values:

```
# Database Configuration
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=fileflow
MYSQL_USER=fileflow
MYSQL_PASSWORD=fileflow

# JWT Configuration
JWT_SECRET=dev_secret_not_for_production
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Firebase Configuration (optional for dev)
FIREBASE_ENABLED=false
```

### 2. Firebase Configuration (Optional)

If you want to use Firebase authentication in development:

1. Set `FIREBASE_ENABLED=true` in your `.env` file
2. Place your `firebase-service-account.json` in `src/main/resources/`

### 3. Start Development Dependencies

Start the required services (MySQL, MinIO, Elasticsearch, Redis) using Docker Compose:

```bash
# Make the helper script executable
chmod +x docker-compose-helper.sh

# Start development services
./docker-compose-helper.sh dev:start
```

### 4. Run the Application

Run the application with the `dev` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Production Setup

### 1. Create .env File

Create a `.env` file with production values:

```
# Database Configuration
MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_DATABASE=fileflow
MYSQL_USER=fileflow
MYSQL_PASSWORD=strong_password_here
MYSQL_ROOT_PASSWORD=root_password_here

# JWT Configuration
JWT_SECRET=very_long_random_secure_key_here
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Firebase Configuration
FIREBASE_ENABLED=true
FIREBASE_CONFIG_FILE=/app/config/firebase-service-account.json

# Storage Configuration
STORAGE_STRATEGY=minio
MINIO_ACCESS_KEY=minio_access_key_here
MINIO_SECRET_KEY=minio_secret_key_here

# Redis Configuration
REDIS_PASSWORD=redis_password_here

# Other configuration...
```

### 2. Firebase Configuration

1. Create a `config` directory and place your Firebase service account JSON file there:
   ```bash
   mkdir -p config
   cp /path/to/firebase-service-account.json config/
   ```

2. Make sure the file has correct permissions:
   ```bash
   chmod 600 config/firebase-service-account.json
   ```

### 3. Deploy with Docker Compose

Build and start all services:

```bash
# Make the helper script executable
chmod +x docker-compose-helper.sh

# Start production environment
./docker-compose-helper.sh prod:start
```

## Environment Variables Reference

Below are the key environment variables used in the application:

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `MYSQL_HOST` | MySQL database host | `localhost` |
| `MYSQL_PORT` | MySQL database port | `3306` |
| `MYSQL_DATABASE` | MySQL database name | `fileflow` |
| `MYSQL_USER` | MySQL database user | `fileflow` |
| `MYSQL_PASSWORD` | MySQL database password | `fileflow` |
| `JWT_SECRET` | Secret key for JWT signing | - |
| `JWT_EXPIRATION` | JWT token expiration in milliseconds | `86400000` |
| `FIREBASE_ENABLED` | Enable Firebase authentication | `false` |
| `FIREBASE_CONFIG_FILE` | Path to Firebase service account JSON | `classpath:firebase-service-account.json` |
| `STORAGE_STRATEGY` | Storage strategy (`local` or `minio`) | `local` |
| `STORAGE_LOCATION` | Path for local file storage | `fileflow-storage` |
| `MINIO_ENDPOINT` | MinIO server endpoint | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | `minioadmin` |
| `MINIO_SECRET_KEY` | MinIO secret key | `minioadmin` |
| `REDIS_HOST` | Redis server host | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |
| `REDIS_PASSWORD` | Redis server password | `fileflow` |

## Troubleshooting

### Common Issues

1. **Environment variables not loading**:
    - Check the file path and permissions of your `.env` file
    - Ensure correct format (KEY=value without spaces around =)
    - Check the logs for initialization messages

2. **Database connection issues**:
    - Verify that the MySQL container is running: `docker ps`
    - Check the database credentials in your `.env` file
    - Ensure the database port is accessible (not blocked by firewall)

3. **Firebase initialization errors**:
    - Check that the service account file exists and has correct permissions
    - Verify the file contains valid JSON
    - Enable debug logging: `logging.level.com.google.firebase=DEBUG`

## Helper Script Commands

```bash
# Development environment
./docker-compose-helper.sh dev:start     # Start development services
./docker-compose-helper.sh dev:stop      # Stop development services
./docker-compose-helper.sh dev:restart   # Restart development services

# Production environment
./docker-compose-helper.sh prod:start    # Start production environment
./docker-compose-helper.sh prod:stop     # Stop production environment
./docker-compose-helper.sh prod:restart  # Restart production environment

# View logs
./docker-compose-helper.sh logs dev      # View all development logs
./docker-compose-helper.sh logs dev mysql # View MySQL development logs
./docker-compose-helper.sh logs prod     # View all production logs
./docker-compose-helper.sh logs prod app # View application production logs
```