# FileFlow - Advanced File Storage and Management System

FileFlow is a comprehensive cloud storage solution that provides secure file storage, user authentication, access control, and efficient file management. It handles file uploads/downloads, folder organization, sharing permissions, and data security, ensuring a seamless user experience comparable to Google Drive or Dropbox.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![MinIO](https://img.shields.io/badge/MinIO-Latest-yellow.svg)](https://min.io/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-7.17-purple.svg)](https://www.elastic.co/)
[![Firebase](https://img.shields.io/badge/Firebase-Auth-orange.svg)](https://firebase.google.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Development Environment](#development-environment)
- [Production Deployment](#production-deployment)
- [Environment Configuration](#environment-configuration)
- [Authentication](#authentication)
- [Testing](#testing)
- [API Documentation](#api-documentation)
- [Contributing](#contributing)
- [Documentation](#documentation)

## Features

### Core Functionality

- **User Authentication & Authorization**:
    - Traditional JWT-based authentication
    - Social login via Firebase Authentication (Google, GitHub, Microsoft, Apple)
    - Secure password management and reset flow
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
- **Multi-provider Authentication**: Seamless integration with social logins
- **Distributed Caching**: Redis-based caching for improved performance

## Architecture

FileFlow is built with a modular, layered architecture that promotes separation of concerns and flexibility.

### Storage Layer

- **Storage Service Abstraction** with multiple implementations:
    - `LocalEnhancedStorageService`: File system-based implementation for development
    - `MinioEnhancedStorageService`: Object storage implementation using MinIO for production

### Authentication Layer

- **Multiple Authentication Methods**:
    - Traditional username/password with JWT tokens
    - Firebase Authentication for social login providers
    - Centralized token management with refresh token support

### Service Layer

Dedicated services for each major feature:

- `FileService`: Core file operations (upload, download, move, etc.)
- `FolderService`: Folder management
- `VersioningService`: File versioning
- `TagService`: File tagging
- `QuotaService`: Storage quota management
- `SearchService`: File search capabilities
- `ActivityService`: User activity tracking
- `AuthService`: Authentication management
- `FirebaseAuthService`: Social login integration

### Data Layer

- **JPA/Hibernate** for relational database storage
- **Elasticsearch** for full-text search capabilities
- **Redis** for distributed caching and token management

### API Layer

RESTful APIs for:

- File and folder management
- Search functionality
- User management
- Quota management
- Authentication and social login

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
- **Authentication**:
    - JWT (JSON Web Tokens)
    - Firebase Authentication
- **Caching**: Redis 7.0
- **API Documentation**: OpenAPI/Swagger
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito, Spring Test
- **Environment Management**: java-dotenv

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

2. Set up your environment:
   ```
   cp .env.example .env.dev  # For development
   cp .env.dev .env
   ```

3. Run the application with default development settings:
   ```
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. The application will be available at http://localhost:8080

For a more detailed setup guide, please refer to the [Setup Guide](docs/SETUP.md).

## Development Environment

For local development, you can use:

### Option 1: Start Only Dependencies

Run only the infrastructure dependencies (MySQL, MinIO, Elasticsearch, Redis) in Docker, while running the application locally:

```bash
# Use our helper script
chmod +x docker-compose-helper.sh
./docker-compose-helper.sh dev:start

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Option 2: Environment Configuration

1. Copy the development environment template:
   ```bash
   cp .env.example .env.dev
   cp .env.dev .env
   ```

2. Start the dependencies:
   ```bash
   docker-compose -f docker-compose-dev.yml up -d
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

### Option 3: Manual Setup

For details on setting up the development environment manually without Docker, see the [Manual Setup Guide](docs/MANUAL_SETUP.md).

## Production Deployment

For production deployment with Docker:

```bash
# Set up your production environment
cp .env.example .env
# Edit .env with production values

# Start all services
docker-compose up -d
```

See the [Production Deployment Guide](docs/PRODUCTION.md) for detailed production deployment instructions.

## Environment Configuration

FileFlow uses a flexible environment configuration system that supports multiple deployment scenarios.

### Environment Variables

Configuration can be provided via:

1. **System Environment Variables**
2. **.env Files** - For local configuration
3. **application.properties** - Default values

### Configuration Files

- **.env** - Main environment file
- **.env.dev** - Development-specific settings
- **.env.example** - Template with all available options
- **application.properties** - Base application settings
- **application-dev.properties** - Development profile settings
- **application-prod.properties** - Production profile settings

For detailed information on configuring your environment, see the [Environment Configuration Guide](docs/ENVIRONMENT.md).

## Authentication

FileFlow supports multiple authentication methods:

### Traditional Authentication

- Username/password authentication
- JWT token-based authentication
- Refresh token mechanism for extended sessions

### Social Authentication

- Integration with Firebase Authentication
- Support for Google, GitHub, Microsoft, and Apple login
- Seamless account linking between traditional and social accounts

### Configuration

To enable social authentication:

1. Set up a Firebase project
2. Configure service account credentials
3. Enable desired providers (Google, GitHub, etc.)
4. Set `FIREBASE_ENABLED=true` in your environment

For detailed setup instructions, see the [Authentication Guide](docs/AUTHENTICATION.md).

## Testing

FileFlow includes comprehensive tests to ensure functionality and stability:

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=FileControllerTest
```

### Test Categories

- **Unit Tests**: Tests for service classes and utilities
- **Controller Tests**: Tests for REST controllers
- **Security Tests**: Tests for authentication and authorization
- **Integration Tests**: End-to-end feature testing

For more details on testing, see the [Testing Guide](docs/TESTING.md).

## API Documentation

When the application is running, API documentation is available at:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Configuration

Configuration options can be overridden using environment variables, `.env` files, or in:

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

# Firebase Authentication
app.firebase.enabled=true
app.firebase.config-file=classpath:firebase-service-account.json
```

## Documentation

For more detailed documentation, refer to:

- [Setup Guide](docs/SETUP.md) - Detailed setup and deployment instructions
- [Manual Setup Guide](docs/MANUAL_SETUP.md) - Running without Docker
- [Environment Configuration Guide](docs/ENVIRONMENT.md) - Configure your environment
- [Authentication Guide](docs/AUTHENTICATION.md) - Authentication setup and configuration
- [Production Deployment Guide](docs/PRODUCTION.md) - Production deployment best practices
- [Testing Guide](docs/TESTING.md) - Testing strategy and procedures
- [Search Integration](docs/SEARCH_INTEGRATION.md) - Documentation on the Elasticsearch integration
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
- Firebase for authentication
- MinIO for object storage
- Elasticsearch for search capabilities
- Redis for caching
- All open source libraries used in this project