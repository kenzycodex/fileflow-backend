# Running FileFlow Manually (Without Docker)

This guide provides detailed instructions for setting up and running the FileFlow application manually without using Docker containers.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Manual Installation Steps](#manual-installation-steps)
- [Environment Configuration](#environment-configuration)
- [Running Individual Components](#running-individual-components)
- [Authentication Setup](#authentication-setup)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Before setting up FileFlow manually, ensure you have the following installed:

- Java 17 or later
- Maven 3.6.3 or later (or use the included Maven wrapper)
- MySQL 8.0
- Elasticsearch 7.17 (optional but recommended for search capabilities)
- MinIO (optional, can use local file system storage instead)
- Redis 7.0+ (optional, for production caching)
- Firebase project (optional, for social login)

## Manual Installation Steps

### 1. Setting Up MySQL

1. Install MySQL 8.0 from the [official website](https://dev.mysql.com/downloads/mysql/)

2. Start the MySQL service:
   ```bash
   # Linux
   sudo systemctl start mysql
   
   # macOS
   brew services start mysql
   
   # Windows
   net start mysql
   ```

3. Create the database and user:
   ```sql
   CREATE DATABASE IF NOT EXISTS fileflow;
   CREATE USER 'fileflow'@'localhost' IDENTIFIED BY 'fileflow';
   GRANT ALL PRIVILEGES ON fileflow.* TO 'fileflow'@'localhost';
   FLUSH PRIVILEGES;
   ```

### 2. Setting Up Elasticsearch (Optional)

1. Download Elasticsearch 7.17 from the [official website](https://www.elastic.co/downloads/past-releases/elasticsearch-7-17-0)

2. Extract the archive and navigate to the directory:
   ```bash
   tar -xzf elasticsearch-7.17.0-linux-x86_64.tar.gz
   cd elasticsearch-7.17.0
   ```

3. Start Elasticsearch:
   ```bash
   ./bin/elasticsearch
   ```

4. Verify Elasticsearch is running:
   ```bash
   curl http://localhost:9200
   ```

### 3. Setting Up MinIO (Optional)

1. Download MinIO from the [official website](https://min.io/download)

2. Create a directory for MinIO data:
   ```bash
   mkdir -p ~/minio-data
   ```

3. Start MinIO server:
   ```bash
   minio server ~/minio-data --console-address ":9001"
   ```

4. Access the MinIO console at http://localhost:9001 (default credentials: minioadmin/minioadmin)

5. Create a bucket named `fileflow` for the application

### 4. Setting Up Redis (Optional)

1. Install Redis:
   ```bash
   # Linux
   sudo apt-get install redis-server
   
   # macOS
   brew install redis
   
   # Windows
   # Download and install from https://github.com/microsoftarchive/redis/releases
   ```

2. Start Redis:
   ```bash
   # Linux
   sudo systemctl start redis
   
   # macOS
   brew services start redis
   
   # Windows
   redis-server
   ```

3. Verify Redis is running:
   ```bash
   redis-cli ping
   ```
   You should receive a `PONG` response.

### 5. Firebase Setup (Optional, for Social Login)

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)

2. Enable Authentication and the desired providers (Google, GitHub, Microsoft, Apple)

3. Generate a service account key:
   - Go to Project Settings > Service Accounts
   - Click "Generate new private key"
   - Save the JSON file securely

4. Place the service account key file in your project:
   ```bash
   mkdir -p src/main/resources
   cp /path/to/firebase-service-account.json src/main/resources/
   ```

## Environment Configuration

### 1. Setting Up Environment Variables

Create a `.env` file in the project root directory with the following content:

```
# Database Configuration
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=fileflow
MYSQL_USER=fileflow
MYSQL_PASSWORD=fileflow

# JWT Configuration
JWT_SECRET=your_secure_jwt_secret_key_for_development
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Storage Configuration
STORAGE_STRATEGY=local
STORAGE_LOCATION=fileflow-storage-dev

# MinIO Configuration (if using MinIO)
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=fileflow

# Elasticsearch Configuration (if using Elasticsearch)
ELASTICSEARCH_HOST=localhost
ELASTICSEARCH_PORT=9200
ELASTICSEARCH_USERNAME=elasticadmin
ELASTICSEARCH_PASSWORD=elasticadmin

# Redis Configuration (if using Redis)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Firebase Configuration (if using Firebase)
FIREBASE_ENABLED=false
FIREBASE_CONFIG_FILE=classpath:firebase-service-account.json
```

### 2. Application Properties

The default `application.properties` and profile-specific properties (`application-dev.properties`, `application-prod.properties`) should work with the environment variables defined above. You can find these files in `src/main/resources/`.

## Running the Application

### 1. Clone the Repository

```bash
git clone https://github.com/kenzycodex/fileflow-backend.git
cd fileflow-backend
```

### 2. Build the Application

```bash
./mvnw clean package -DskipTests
```

### 3. Run the Application

Run with the development profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will be available at http://localhost:8080

## Running Individual Components

### Running Only with MySQL (Basic Setup)

1. Configure local storage in your `.env` file:
   ```
   STORAGE_STRATEGY=local
   STORAGE_LOCATION=fileflow-storage-dev
   FIREBASE_ENABLED=false
   ```

2. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Running with Elasticsearch

1. Start Elasticsearch as described above

2. Update your `.env` file:
   ```
   ELASTICSEARCH_HOST=localhost
   ELASTICSEARCH_PORT=9200
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Running with MinIO

1. Start MinIO as described above

2. Update your `.env` file:
   ```
   STORAGE_STRATEGY=minio
   MINIO_ENDPOINT=http://localhost:9000
   MINIO_ACCESS_KEY=minioadmin
   MINIO_SECRET_KEY=minioadmin
   MINIO_BUCKET=fileflow
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Running with Redis

1. Start Redis as described above

2. Update your `.env` file:
   ```
   REDIS_HOST=localhost
   REDIS_PORT=6379
   REDIS_PASSWORD=
   CACHE_TYPE=redis
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## Authentication Setup

### JWT Authentication (Default)

The standard username/password authentication with JWT tokens works out of the box with the configuration provided above.

### Firebase Social Login

To enable Firebase authentication:

1. Follow the Firebase setup steps described earlier

2. Update your `.env` file:
   ```
   FIREBASE_ENABLED=true
   FIREBASE_CONFIG_FILE=classpath:firebase-service-account.json
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. Test the Firebase authentication endpoints:
   - POST to `/api/v1/auth/social/firebase` with a valid Firebase ID token
   - The token should be obtained from Firebase on the client side

## Access Points

- **Application**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **MinIO Console**: http://localhost:9001
- **Elasticsearch**: http://localhost:9200

## Troubleshooting

### MySQL Connection Issues

- Verify MySQL is running: `ps aux | grep mysql` or `systemctl status mysql`
- Check database credentials in `.env` file
- Ensure the database exists: `mysql -u root -p -e "SHOW DATABASES LIKE 'fileflow';"`

### Elasticsearch Issues

- Verify Elasticsearch is running: `curl http://localhost:9200`
- Check Elasticsearch logs: `tail -f ~/elasticsearch-7.17.0/logs/elasticsearch.log`
- If you get connection errors, ensure Elasticsearch is listening on the correct interface

### MinIO Issues

- Verify MinIO is running: `ps aux | grep minio`
- Check if the bucket exists by accessing the MinIO console: http://localhost:9001
- Ensure credentials in `.env` file match MinIO's credentials

### Redis Issues

- Verify Redis is running: `redis-cli ping`
- Check Redis logs: `tail -f /var/log/redis/redis-server.log` (path may vary)
- Try connecting manually: `redis-cli`

### Firebase Authentication Issues

- Verify the Firebase service account file exists and is correctly placed
- Check the file permissions: the file should be readable
- Ensure Firebase Authentication is enabled in the Firebase Console
- Verify the providers (Google, GitHub, etc.) are enabled
- Enable debug logging by adding this to your `application-dev.properties`:
  ```properties
  logging.level.com.fileflow.config=DEBUG
  logging.level.com.google.firebase=DEBUG
  ```

### Environment Variables Not Loading

- Verify the `.env` file is in the project root directory
- Check file permissions: the file should be readable
- Ensure the format is correct (KEY=value with no spaces around =)
- Try setting environment variables directly:
  ```bash
  export FIREBASE_ENABLED=true
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```

### Application Won't Start

- Check application logs for specific errors:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dlogging.level.com.fileflow=DEBUG
  ```
- Verify all required services are running
- Make sure the storage directory exists and has proper permissions
- Ensure no port conflicts (default port is 8080)