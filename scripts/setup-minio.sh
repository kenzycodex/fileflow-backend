#!/bin/bash

# This script sets up MinIO for FileFlow

# MinIO configuration
MINIO_USER="minioadmin"
MINIO_PASS="minioadmin"
MINIO_ENDPOINT="http://localhost:9000"
BUCKET_NAME="fileflow"

# Check if MinIO client is installed
if ! command -v mc &> /dev/null; then
    echo "MinIO client (mc) is not installed."
    echo "Please install it from https://docs.min.io/docs/minio-client-quickstart-guide.html"
    exit 1
fi

# Configure MinIO client
echo "Configuring MinIO client..."
mc config host add myminio ${MINIO_ENDPOINT} ${MINIO_USER} ${MINIO_PASS}

# Create bucket if it doesn't exist
echo "Creating bucket ${BUCKET_NAME} (if not exists)..."
mc mb --ignore-existing myminio/${BUCKET_NAME}

# Set bucket policy to allow public access (optional, depends on your requirements)
echo "Setting bucket policy..."
cat > /tmp/fileflow-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": ["*"]
      },
      "Action": [
        "s3:GetObject"
      ],
      "Resource": [
        "arn:aws:s3:::${BUCKET_NAME}/*"
      ]
    }
  ]
}
EOF

mc policy set /tmp/fileflow-policy.json myminio/${BUCKET_NAME}

echo "MinIO setup complete."
echo "Bucket ${BUCKET_NAME} is now ready to use."