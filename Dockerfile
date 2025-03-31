FROM amazoncorretto:17-alpine as build

WORKDIR /app

# Copy maven files first for better caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make the mvnw script executable
RUN chmod +x ./mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

FROM amazoncorretto:17-alpine

WORKDIR /app

# Install required packages
RUN apk add --no-cache curl bash

# Create non-root user
RUN addgroup -S fileflow && adduser -S fileflow -G fileflow

# Create directories
RUN mkdir -p /app/config /data/fileflow-storage /app/logs
RUN chown -R fileflow:fileflow /app /data

# Copy jar file from build stage
COPY --from=build /app/target/*.jar /app/fileflow.jar

# Create a placeholder for the .env file with correct permissions
RUN touch /app/.env && chown fileflow:fileflow /app/.env && chmod 600 /app/.env

# Create a placeholder for the Firebase service account file with correct permissions
RUN touch /app/config/firebase-service-account.json && \
    chown fileflow:fileflow /app/config/firebase-service-account.json && \
    chmod 600 /app/config/firebase-service-account.json

# Switch to non-root user
USER fileflow

# Expose application port
EXPOSE 8080

# Set health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Set entrypoint with environment variables
ENTRYPOINT ["java", "-jar", "/app/fileflow.jar"]