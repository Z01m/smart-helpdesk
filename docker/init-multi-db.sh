#!/bin/bash

set -e

databases=(
  "gateway"
  "ai_worker"
  "notification"
  "knowledge_base"
)

for database in "${databases[@]}"; do
  echo "Creating database: $database"

  psql \
    -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "postgres" \
    <<-EOSQL
        CREATE DATABASE $database;
EOSQL
done

echo "All databases created successfully"