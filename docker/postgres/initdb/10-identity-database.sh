#!/bin/sh
# Creates the identity service's database and the role that owns it, next to the monolith's
# "bookland" database in the same PostgreSQL. One database per service: the identity service's
# credentials open only its own, and the monolith's never see users or oauth2_*.
#
# The postgres image runs this ONLY when the data volume is empty (first start). An existing volume
# keeps whatever it had: to apply it, recreate the volume with `docker compose down -v`.
set -eu

psql -v ON_ERROR_STOP=1 \
     -v identity_user="$IDENTITY_DB_USER" \
     -v identity_password="$IDENTITY_DB_PASSWORD" \
     --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
CREATE ROLE :"identity_user" LOGIN PASSWORD :'identity_password';
CREATE DATABASE identity OWNER :"identity_user";
EOSQL
