#!/bin/bash
set -e

# Environment variables from .env
DB_USER=${POSTGRES_USER:-admin}

# Application databases
DATABASES=(userdb gatewaydb)

# Create databases (connect to 'postgres' default database)
for DB in "${DATABASES[@]}"; do
    echo "Creating database: $DB"
    psql -v ON_ERROR_STOP=1 --username "$DB_USER" -d postgres <<-EOSQL
        CREATE DATABASE "$DB";
        GRANT ALL PRIVILEGES ON DATABASE "$DB" TO "$DB_USER";
EOSQL
done

echo "✅ All databases created successfully!"
