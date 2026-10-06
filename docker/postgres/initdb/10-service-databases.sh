#!/bin/sh
# One database per service in this PostgreSQL, each owned by a role of its own that can connect to
# nothing else:
#
#   bookland  (created by the image from POSTGRES_DB) -> owned by APP_DB_USER       (the monolith)
#   identity  (created here)                          -> owned by IDENTITY_DB_USER  (identity service)
#
# POSTGRES_USER is the image's superuser. No service connects with it: it can read every database,
# so a service holding it would see the others' data. It stays for administration only.
#
# Owning a database makes a role the owner of its "public" schema (PostgreSQL 15+: the schema
# belongs to pg_database_owner), which is what lets each service's Flyway create its tables.
# CONNECT is revoked from PUBLIC — by default every role may connect to every database, and only
# table privileges stood in the way.
#
# The postgres image runs this ONLY when the data volume is empty (first start). An existing volume
# keeps whatever it had: to apply it, recreate the volume with `docker compose down -v`.
set -eu

psql -v ON_ERROR_STOP=1 \
     -v app_user="$APP_DB_USER" \
     -v app_password="$APP_DB_PASSWORD" \
     -v app_database="$POSTGRES_DB" \
     -v identity_user="$IDENTITY_DB_USER" \
     -v identity_password="$IDENTITY_DB_PASSWORD" \
     --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';
ALTER DATABASE :"app_database" OWNER TO :"app_user";
REVOKE CONNECT ON DATABASE :"app_database" FROM PUBLIC;

CREATE ROLE :"identity_user" LOGIN PASSWORD :'identity_password';
CREATE DATABASE identity OWNER :"identity_user";
REVOKE CONNECT ON DATABASE identity FROM PUBLIC;
EOSQL
