#!/bin/bash

read -p "Username (default 'niordadmin'): " KEYCLOAK_ADMIN_USER
KEYCLOAK_ADMIN_USER=${KEYCLOAK_ADMIN_USER:-niordadmin}

read -s -p "Password (default 'keycloak'): " KEYCLOAK_ADMIN_PWD
KEYCLOAK_ADMIN_PWD=${KEYCLOAK_ADMIN_PWD:-keycloak}

echo "Adding keycloak admin user $KEYCLOAK_ADMIN_USER"
docker exec niord-keycloak keycloak/bin/add-user-keycloak.sh -r master -u "$KEYCLOAK_ADMIN_USER" -p "$KEYCLOAK_ADMIN_PWD"
docker restart niord-keycloak

exit 0
