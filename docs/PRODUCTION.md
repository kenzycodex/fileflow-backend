# Production Deployment Guide

This guide covers the steps and best practices for deploying FileFlow in a production environment.

## Deployment Options

FileFlow can be deployed using:

1. **Docker Containers** (recommended)
2. **Manual Deployment** (advanced users)

## Docker Deployment

### Prerequisites

- Docker Engine 20.10.x or later
- Docker Compose 2.x or later
- At least 4GB RAM, 2 CPU cores
- 20GB+ storage space

### Deployment Steps

#### 1. Prepare Environment Configuration

Create a production environment file:

```bash
# Start with the example template
cp .env.example .env

# Edit with your production values
editor .env
```

Ensure these key settings for production:

```properties
# Set production profile
SPRING_PROFILES_ACTIVE=prod

# Use secure passwords
MYSQL_PASSWORD=strong_production_password
MYSQL_ROOT_PASSWORD=different_strong_password
REDIS_PASSWORD=another_strong_password

# Configure proper storage
STORAGE_STRATEGY=minio
STORAGE_LOCATION=/data/fileflow-storage

# Set up email for notifications
MAIL_HOST=your-smtp-server.com
MAIL_USERNAME=your-email@domain.com
MAIL_PASSWORD=your-smtp-password

# Social login (if used)
FIREBASE_ENABLED=true
FIREBASE_CONFIG_FILE=/app/config/firebase-service-account.json
```

#### 2. Prepare Firebase Configuration (if used)

If using Firebase Authentication:

1. Download your service account key from Firebase Console
2. Create a config directory and place the file there:
   ```bash
   mkdir -p config
   cp path/to/firebase-service-account.json config/
   
   # Set proper permissions
   chmod 600 config/firebase-service-account.json
   ```

#### 3. Start the Application

```bash
# Start all services
docker-compose up -d
```

#### 4. Verify Deployment

```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs -f app
```

Access the application at http://your-server-ip:8080

### Health Monitoring

Use the built-in health endpoint to monitor application health:

```bash
curl http://your-server-ip:8080/actuator/health
```

## Production Considerations

### Security

#### TLS/SSL Configuration

For HTTPS support, set up SSL in your environment:

```properties
# SSL Configuration
SSL_ENABLED=true
SSL_KEYSTORE_PATH=/app/config/keystore.p12
SSL_KEYSTORE_PASSWORD=your_keystore_password
SSL_KEYSTORE_TYPE=PKCS12
SSL_KEY_ALIAS=fileflow
```

Or use a reverse proxy like Nginx for SSL termination.

#### Firewall Rules

Configure firewall to only expose necessary ports:

- 8080: Application (if not behind a proxy)
- 443: HTTPS (if using SSL)

Keep all other services (MySQL, Redis, etc.) restricted to internal network.

### Database

#### Backup Strategy

Set up regular database backups:

```bash
# Example backup script
docker exec fileflow-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} fileflow > backup_$(date +%F).sql
```

Consider using a cron job for regular backups:

```
0 2 * * * /path/to/backup-script.sh
```

#### Connection Pool

Optimize connection pool settings for your server:

```properties
HIKARI_MAX_POOL_SIZE=20
HIKARI_MIN_IDLE=5
HIKARI_CONNECTION_TIMEOUT=30000
```

### Storage Configuration

For production, use MinIO for reliable, S3-compatible storage:

```properties
STORAGE_STRATEGY=minio
MINIO_ENDPOINT=http://minio:9000
```

Ensure MinIO data is backed up or replicated.

### Performance Tuning

#### JVM Settings

Optimize JVM for your hardware:

```
JAVA_OPTS=-Xms1G -Xmx2G -XX:+UseG1GC
```

Add these settings to your Docker Compose file.

#### Caching

Enable Redis caching for improved performance:

```properties
CACHE_TYPE=redis
REDIS_HOST=redis
```

### Scalability

FileFlow can be scaled horizontally:

1. Use a load balancer (e.g., Nginx, HAProxy)
2. Share the same database and object storage
3. Use Redis for distributed caching and session management

### Logging

Configure appropriate logging:

```properties
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_FILEFLOW=INFO
LOG_FILE_NAME=/app/logs/fileflow.log
```

Consider integrating with log management systems like ELK Stack (Elasticsearch, Logstash, Kibana) or Graylog.

## Docker Compose Resource Limits

Set resource limits in `docker-compose.yml` to prevent container issues:

```yaml
services:
  app:
    # ...other configuration
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### Monitoring

Monitor your application using:

1. Spring Boot Actuator endpoints
2. Prometheus and Grafana
3. Container monitoring tools

Enable appropriate monitoring endpoints:

```properties
# Actuator Configuration
ACTUATOR_ENDPOINTS=health,info,metrics,prometheus
```

## Maintenance Procedures

### Updates and Upgrades

To update the application:

```bash
# Pull the latest changes
git pull

# Build the application
./mvnw clean package -DskipTests

# Rebuild and restart containers
docker-compose down
docker-compose build --no-cache app
docker-compose up -d
```

### Database Migrations

Database migrations are handled automatically by Flyway when the application starts. For sensitive migrations, consider:

1. Taking a database backup first
2. Testing migrations in a staging environment
3. Scheduling migrations during low-traffic periods

## Disaster Recovery

### Backup Strategy

1. **Database**: Daily SQL dumps
2. **Object Storage**: Regular MinIO bucket backups
3. **Configuration**: Backup of all environment files and certificates

### Recovery Procedure

1. Restore database from backup
2. Restore MinIO data
3. Redeploy application with configuration
4. Verify system integrity

## Manual Deployment

For environments where Docker is not an option:

### Requirements

- JDK 17
- MySQL 8.0
- Redis (optional but recommended)
- MinIO (or compatible S3 service)
- Elasticsearch (optional)

### Deployment Steps

1. **Build the application**:
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. **Create a service**:
   ```
   [Unit]
   Description=FileFlow Application
   After=network.target mysql.service

   [Service]
   User=fileflow
   WorkingDirectory=/opt/fileflow
   ExecStart=/usr/bin/java -jar /opt/fileflow/fileflow.jar --spring.profiles.active=prod
   EnvironmentFile=/opt/fileflow/.env
   Restart=always

   [Install]
   WantedBy=multi-user.target
   ```

3. **Start the service**:
   ```bash
   systemctl enable fileflow
   systemctl start fileflow
   ```

## Troubleshooting

### Common Production Issues

#### Application Won't Start

Check logs for errors:
```bash
docker-compose logs app
```

Common causes:
- Database connection issues
- Insufficient memory
- Permission problems with mounted volumes

#### High Memory Usage

1. Check for memory leaks
2. Adjust JVM memory settings
3. Monitor with `docker stats`

#### Slow Performance

1. Enable debug logging temporarily
2. Check database query performance
3. Verify caching is working properly
4. Monitor CPU usage with `docker stats`

### Getting Support

If you encounter issues:
1. Check the documentation and GitHub issues
2. Enable debug logging for the specific component
3. Collect logs and error messages
4. Open an issue with detailed reproduction steps