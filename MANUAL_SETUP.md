# Running FileFlow Manually (Without Docker)

This guide provides detailed instructions for setting up and running the FileFlow application manually without using Docker containers.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Manual Installation Steps](#manual-installation-steps)
- [Running Individual Components](#running-individual-components)
- [Application Configuration](#application-configuration)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Before setting up FileFlow manually, ensure you have the following installed:

- Java 17 or later
- Maven 3.6.3 or later (or use the included Maven wrapper)
- MySQL 8.0
- Elasticsearch 7.17 (optional but recommended for search capabilities)
- MinIO (optional, can use local file system storage instead)

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

### 4. Configuring the Application

1. Clone the FileFlow repository:
   ```bash
   git clone https://github.com/kenzycodex/fileflow-backend.git
   cd fileflow-backend
   ```

2. Update `application-dev.properties` with your local configuration:
   ```properties
   # Database configuration
   spring.datasource.url=jdbc:mysql://localhost:3306/fileflow?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
   spring.datasource.username=fileflow
   spring.datasource.password=fileflow
   
   # Storage configuration
   app.storage.strategy=local
   fileflow.storage.location=fileflow-storage-dev
   
   # MinIO configuration (if using MinIO)
   app.minio.endpoint=http://localhost:9000
   app.minio.access-key=minioadmin
   app.minio.secret-key=minioadmin
   app.minio.bucket=fileflow
   
   # Elasticsearch configuration (if using Elasticsearch)
   elasticsearch.host=localhost
   elasticsearch.port=9200
   ```

### 5. Running the Application

1. Build the application:
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. Run the application with the dev profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   To enable Elasticsearch integration:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,elasticsearch
   ```

3. The application will be available at http://localhost:8080

## Running Individual Components

### Running Only with MySQL (Without Elasticsearch and MinIO)

1. Configure local storage in `application-dev.properties`:
   ```properties
   app.storage.strategy=local
   fileflow.storage.location=fileflow-storage-dev
   ```

2. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Running with MySQL and Elasticsearch

1. Start MySQL and Elasticsearch as described above

2. Run the application with the elasticsearch profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,elasticsearch
   ```

### Running with MySQL and MinIO

1. Start MySQL and MinIO as described above

2. Configure MinIO storage in `application-dev.properties`:
   ```properties
   app.storage.strategy=minio
   app.minio.endpoint=http://localhost:9000
   app.minio.access-key=minioadmin
   app.minio.secret-key=minioadmin
   app.minio.bucket=fileflow
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## Application Configuration

### Key Configuration Properties

#### Storage Configuration

```properties
# Local Storage
app.storage.strategy=local
fileflow.storage.location=fileflow-storage-dev

# MinIO Storage
app.storage.strategy=minio
app.minio.endpoint=http://localhost:9000
app.minio.access-key=minioadmin
app.minio.secret-key=minioadmin
app.minio.bucket=fileflow
```

#### Elasticsearch Configuration

```properties
# Enable Elasticsearch
spring.profiles.active=dev,elasticsearch

# Elasticsearch connection
elasticsearch.host=localhost
elasticsearch.port=9200
```

#### Database Configuration

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/fileflow?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=fileflow
spring.datasource.password=fileflow
```

## Access Points

- **Application**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **MinIO Console**: http://localhost:9001
- **Elasticsearch**: http://localhost:9200

## Troubleshooting

### MySQL Connection Issues

- Verify MySQL is running: `ps aux | grep mysql` or `systemctl status mysql`
- Check database credentials in `application-dev.properties`
- Ensure the database exists: `mysql -u root -p -e "SHOW DATABASES LIKE 'fileflow';"`

### Elasticsearch Issues

- Verify Elasticsearch is running: `curl http://localhost:9200`
- Check Elasticsearch logs: `tail -f ~/elasticsearch-7.17.0/logs/elasticsearch.log`
- If you get connection errors, ensure Elasticsearch is listening on the correct interface

### MinIO Issues

- Verify MinIO is running: `ps aux | grep minio`
- Check if the bucket exists by accessing the MinIO console: http://localhost:9001
- Ensure credentials in `application-dev.properties` match MinIO's credentials

### Application Won't Start

- Check the application logs for specific errors
- Verify all required services are running
- Make sure the storage directory exists and has proper permissions
- Try running with verbose logging: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dlogging.level.com.fileflow=DEBUG`