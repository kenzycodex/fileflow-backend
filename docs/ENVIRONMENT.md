# Environment Configuration Guide

FileFlow offers a flexible configuration system that allows you to easily set up different environments with appropriate settings. This guide explains how to configure and manage environments in FileFlow.

## Configuration System Overview

FileFlow uses a three-tier approach to configuration:

1. **System Environment Variables** (highest priority)
2. **.env Files** (middle priority)
3. **application.properties** (lowest priority)

This approach allows you to:
- Use sensible defaults for most settings
- Override settings for different environments
- Keep sensitive information secure

## Environment Files

### Available Environment Files

- **`.env`**: The main environment file, used by default
- **`.env.dev`**: Development-specific settings
- **`.env.example`**: Template showing all available options

### File Locations

- **Development**: Place in the project root directory
- **Production**: Mount to `/app/.env` in the Docker container

### Using Multiple Environment Files

You can maintain separate files for different environments:

```bash
# Start with the example template
cp .env.example .env.dev
cp .env.example .env.prod

# Customize each file for its environment
editor .env.dev   # Edit development settings
editor .env.prod  # Edit production settings

# Activate the environment you want to use
cp .env.dev .env  # Use development settings
# or
cp .env.prod .env  # Use production settings
```

## Spring Profiles

FileFlow uses Spring profiles to activate environment-specific configurations:

- **`dev`**: Development environment
- **`prod`**: Production environment

### Setting the Active Profile

Set the active profile using:

1. Environment variable:
   ```
   SPRING_PROFILES_ACTIVE=dev
   ```

2. Java system property:
   ```bash
   ./mvnw spring-boot:run -Dspring.profiles.active=dev
   ```

3. In your `.env` file:
   ```
   SPRING_PROFILES_ACTIVE=dev
   ```

## Configuration Categories

### Database Connection

```properties
# MySQL Configuration
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=fileflow
MYSQL_USER=fileflow
MYSQL_PASSWORD=secure_password

# Connection Pool Settings
HIKARI_MAX_POOL_SIZE=10
HIKARI_MIN_IDLE=5
HIKARI_IDLE_TIMEOUT=30000
```

### Authentication

```properties
# JWT Settings
JWT_SECRET=your_secure_jwt_secret_key
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Firebase Authentication
FIREBASE_ENABLED=true
FIREBASE_CONFIG_FILE=/path/to/firebase-service-account.json
```

### Storage Configuration

```properties
# Storage Strategy
STORAGE_STRATEGY=local  # 'local' or 'minio'
STORAGE_LOCATION=fileflow-storage

# MinIO Settings (if using MinIO)
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=fileflow
```

### Caching with Redis

```properties
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
CACHE_TYPE=simple  # 'simple' or 'redis'
```

### Full-Text Search

```properties
# Elasticsearch Configuration
ELASTICSEARCH_HOST=localhost
ELASTICSEARCH_PORT=9200
ELASTICSEARCH_USERNAME=elasticadmin
ELASTICSEARCH_PASSWORD=elasticadmin
```

### Mail Settings

```properties
# Mail Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

### Logging

```properties
# Logging Configuration
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_SPRING_WEB=INFO
LOG_LEVEL_SPRING_SECURITY=INFO
LOG_LEVEL_FILEFLOW=INFO
LOG_FILE_NAME=logs/fileflow.log
```

## Environment-Specific Configurations

### Development Environment

Optimized for local development:

```properties
# General
SPRING_PROFILES_ACTIVE=dev

# Storage
STORAGE_STRATEGY=local
STORAGE_LOCATION=fileflow-storage-dev

# Database
MYSQL_HOST=localhost

# Logging (verbose for development)
LOG_LEVEL_FILEFLOW=DEBUG
LOG_LEVEL_SPRING_WEB=DEBUG

# Firebase (often disabled for development)
FIREBASE_ENABLED=false

# Disable actual email sending
MAIL_HOST=localhost
MAIL_PORT=3025
```

### Production Environment

Optimized for security and performance:

```properties
# General
SPRING_PROFILES_ACTIVE=prod

# Storage
STORAGE_STRATEGY=minio
STORAGE_LOCATION=/data/fileflow-storage

# Security
SSL_ENABLED=true
RATE_LIMITING_ENABLED=true

# Database (container services)
MYSQL_HOST=mysql
REDIS_HOST=redis
ELASTICSEARCH_HOST=elasticsearch

# Logging (minimal for production)
LOG_LEVEL_FILEFLOW=INFO
LOG_LEVEL_SPRING_WEB=INFO
```

## Docker Environment Integration

### Development Mode

Run dependencies in Docker, but the application locally:

```bash
# Start dependencies only
docker-compose -f docker-compose-dev.yml up -d

# Run application with development profile
./mvnw spring-boot:run -Dspring.profiles.active=dev
```

### Production Mode

Run everything in Docker with production settings:

```bash
# Ensure .env has production settings
cp .env.prod .env

# Start all services including the application
docker-compose up -d
```

## Environment Variable Handling

The application loads environment variables in this order:

1. Default values in `application.properties`
2. Profile-specific values from `application-{profile}.properties`
3. Environment variables from `.env` file (using java-dotenv)
4. System environment variables

This means that system environment variables take precedence over `.env` file, which takes precedence over properties files.

## Sensitive Information

### Security Best Practices

- **Never commit** `.env` files or service account keys to version control
- Use `.gitignore` to exclude sensitive files
- Use different credentials for development and production
- For production, consider using secrets management services

### Recommended .gitignore Rules

```
# Environment files
.env
.env.*
!.env.example

# Firebase credentials
firebase-service-account.json
**/firebase-service-account.json
```

## Troubleshooting

### Common Issues

1. **Configuration not being applied**:
    - Check the loading order and if a higher-priority setting is overriding it
    - Verify the correct profile is active
    - Check for typos in variable names

2. **Environment variables not loading from .env**:
    - Ensure `.env` file is in the correct location
    - Check file format (KEY=value with no spaces around =)
    - Verify file permissions allow reading

3. **Docker container can't access environment variables**:
    - Ensure the `.env` file is correctly mounted
    - For Docker Compose, check the `env_file` setting

4. **Profile-specific properties not taking effect**:
    - Verify the active profile (`SPRING_PROFILES_ACTIVE`)
    - Check that the profile-specific properties file exists

### Enabling Debug Logging

To troubleshoot environment loading issues, enable debug logging:

```properties
# Add to application.properties or .env
logging.level.com.fileflow.config=DEBUG
logging.level.io.github.cdimascio.dotenv=DEBUG
```