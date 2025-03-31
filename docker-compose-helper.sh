#!/bin/bash

# Helper script for running Docker Compose commands with the correct environment

# Functions
start_dev() {
  echo "Starting development services..."
  docker-compose -f docker-compose-dev.yml up -d
  echo "Development services started successfully!"
  echo "Connect to MySQL at localhost:3306"
  echo "Connect to MinIO at localhost:9000 (API) and localhost:9001 (Console)"
  echo "Connect to Elasticsearch at localhost:9200"
  echo "Connect to Redis at localhost:6379"
}

stop_dev() {
  echo "Stopping development services..."
  docker-compose -f docker-compose-dev.yml down
  echo "Development services stopped!"
}

start_prod() {
  if [ ! -f .env ]; then
    echo "Error: .env file not found! Please create an .env file first."
    echo "You can copy from .env.example and fill in your values."
    exit 1
  fi

  if [ ! -f ./config/firebase-service-account.json ] && [ "$FIREBASE_ENABLED" = "true" ]; then
    echo "Warning: Firebase service account file not found but Firebase is enabled."
    echo "Please place firebase-service-account.json in the ./config directory."
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
    fi
  fi

  echo "Starting production environment..."
  docker-compose up -d
  echo "Production environment started successfully!"
}

stop_prod() {
  echo "Stopping production environment..."
  docker-compose down
  echo "Production environment stopped!"
}

logs() {
  local env=$1
  local service=$2

  if [ "$env" = "dev" ]; then
    docker-compose -f docker-compose-dev.yml logs $service --follow
  else
    docker-compose logs $service --follow
  fi
}

# Main script
case "$1" in
  dev:start)
    start_dev
    ;;

  dev:stop)
    stop_dev
    ;;

  dev:restart)
    stop_dev
    start_dev
    ;;

  prod:start)
    start_prod
    ;;

  prod:stop)
    stop_prod
    ;;

  prod:restart)
    stop_prod
    start_prod
    ;;

  logs)
    if [ -z "$2" ]; then
      echo "Please specify environment (dev or prod)"
      exit 1
    fi

    logs $2 $3
    ;;

  *)
    echo "Usage: $0 {dev:start|dev:stop|dev:restart|prod:start|prod:stop|prod:restart|logs [dev|prod] [service]}"
    exit 1
esac

exit 0