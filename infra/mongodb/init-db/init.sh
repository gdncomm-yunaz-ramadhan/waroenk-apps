#!/bin/bash
set -e

# Read env variables
DB_USER=${MONGO_INITDB_ROOT_USERNAME:-admin}
DB_PASS=${MONGO_INITDB_ROOT_PASSWORD:-admin}

mongosh -u "$DB_USER" -p "$DB_PASS" --authenticationDatabase admin <<EOF
// Create catalog database and user
use catalog
db.createCollection("_init")
db.createUser({
  user: "$DB_USER",
  pwd: "$DB_PASS",
  roles: [{role: "readWrite", db: "catalog"}]
})
print("✅ Catalog database initialized")

// Create productdb for backward compatibility
use productdb
db.createCollection("_init")
db.createUser({
  user: "$DB_USER",
  pwd: "$DB_PASS",
  roles: [{role: "readWrite", db: "productdb"}]
})
print("✅ Productdb database initialized")
EOF
