# FileFlow - Advanced File Storage and Management System

FileFlow is a comprehensive cloud storage solution that provides secure file storage, user authentication, access control, and efficient file management. It handles file uploads/downloads, folder organization, sharing permissions, and data security, ensuring a seamless user experience comparable to Google Drive or Dropbox.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![MinIO](https://img.shields.io/badge/MinIO-Latest-yellow.svg)](https://min.io/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-7.17-purple.svg)](https://www.elastic.co/)

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Development Environment](#development-environment)
- [Production Deployment](#production-deployment)
- [API Documentation](#api-documentation)
- [Contributing](#contributing)
- [Documentation](#documentation)

## Features

### Core Functionality

- **User Authentication & Authorization**: Secure user management with JWT
- **File Operations**: Upload, download, rename, move, and delete files
- **Folder Management**: Create, organize, and manage hierarchical folder structures
- **Trash Management**: Soft deletion with automatic cleanup after configurable retention period
- **Storage Quota**: User-specific storage limits with quota extension capabilities

### Advanced Features

- **Chunked Uploads**: Support for large file uploads with resume capability
- **File Versioning**: Track and restore version history of files
- **File Sharing**: Secure sharing with fine-grained permission controls
- **Tagging System**: Organize files with custom tags
- **File Comments**: Collaborate on files with threaded comments
- **Full-Text Search**: Index and search file content using Elasticsearch
- **File Preview**: Generate thumbnails and previews for common file types
- **File Conversion**: Convert between formats for better interoperability
- **Storage Deduplication**: Hash-based file deduplication to save storage space

## Architecture

FileFlow is built with a modular, layered architecture that promotes separation of concerns and flexibility.

### Storage Layer

- **Storage Service Abstraction** with multiple implementations:
  - `LocalEnhancedStorageService`: File system-based implementation for development
  - `MinioEnhancedStorageService`: Object storage implementation using MinIO for production

### Service Layer

Dedicated services for each major feature:

- `FileService`: Core file operations (upload, download, move, etc.)
- `FolderService`: Folder management
- `VersioningService`: File versioning
- `TagService`: File tagging
- `QuotaService`: Storage quota management
- `SearchService`: File search capabilities
- `ActivityService`: User activity tracking

### Data Layer

- **JPA/Hibernate** for relational database storage
- **Elasticsearch** for full-text search capabilities

### API Layer

RESTful APIs for:

- File and folder management
- Search functionality
- User management
- Quota management

### Database Schema

Key entities in the system:
- `User` - System user with authentication/authorization details
- `File` - Core file metadata
- `Folder` - Folder structure for organizing files
- `FileVersion` - Version history for files
- `Tag` and `FileTag` - Tagging system
- `StorageChunk` - Support for chunked uploads
- `Activity` - Audit logging

## Technology Stack

- **Backend Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: MySQL 8.0
- **Database Migration**: Flyway
- **Object Storage**: MinIO (S3-compatible)
- **Search Engine**: Elasticsearch 7.17
- **Authentication**: JWT (JSON Web Tokens)
- **API Documentation**: OpenAPI/Swagger
- **Build Tool**: Maven

## Getting Started

### Prerequisites

- JDK 17 or later
- Maven 3.6.3 or later
- MySQL 8.0 or later
- Docker and Docker Compose (optional, for containerized deployment)

### Quick Start

1. Clone the repository:
   ```
   git clone https://github.com/kenzycodex/fileflow-backend.git
   cd fileflow-backend
   ```

2. Run the application with default development settings:
   ```
   ./mvnw spring-boot:run
   ```

3. The application will be available at http://localhost:8080

For a more detailed setup guide, please refer to the [Setup Guide](SETUP.md).

## Development Environment

For local development, you can use:

### Option 1: Start Only Dependencies

Run only the infrastructure dependencies (MySQL, MinIO, Elasticsearch) in Docker, while running the application locally:

```bash
docker-compose -f docker-compose-dev.yml up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Option 2: Development Setup Script

Use the included setup script to automatically configure the development environment:

```bash
chmod +x setup-dev.sh
./setup-dev.sh
```

## Production Deployment

For production deployment, use the Docker Compose file:

```bash
docker-compose up -d
```

Configure environment-specific settings in `application-prod.properties` or through environment variables.

See the [Setup Guide](SETUP.md) for detailed production deployment instructions.

## API Documentation

When the application is running, API documentation is available at:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Configuration

Configuration options can be overridden in:

- `application.properties`: Common settings
- `application-dev.properties`: Development settings
- `application-prod.properties`: Production settings

Key configuration options:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/fileflow
spring.datasource.username=root
spring.datasource.password=password

# Storage
app.storage.strategy=local  # or 'minio' for MinIO
app.minio.endpoint=http://localhost:9000
app.minio.access-key=minioadmin
app.minio.secret-key=minioadmin
```

## Documentation

For more detailed documentation, refer to:

- [Setup Guide](SETUP.md) - Detailed setup and deployment instructions
- [Search Integration](SEARCH_INTEGRATION.md) - Documentation on the Elasticsearch integration
- [API Documentation](http://localhost:8080/swagger-ui.html) - Available when the application is running

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to your branch: `git push origin feature/my-feature`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements

- Spring Boot and the Spring community
- MinIO for object storage
- Elasticsearch for search capabilities
- All open source libraries used in this project