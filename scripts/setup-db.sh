#!/bin/bash

# This script sets up the MySQL database for FileFlow

# Database credentials and configuration
DB_USER="root"
DB_PASS="password"
DB_NAME="fileflow"

# Create database if it doesn't exist
echo "Creating database ${DB_NAME} (if not exists)..."
mysql -u ${DB_USER} -p${DB_PASS} -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Grant privileges
echo "Granting privileges..."
mysql -u ${DB_USER} -p${DB_PASS} -e "GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';"
mysql -u ${DB_USER} -p${DB_PASS} -e "FLUSH PRIVILEGES;"

echo "Database setup complete."
echo "NOTE: Flyway will handle schema initialization and migrations."