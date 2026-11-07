#!/bin/sh
set -eu
issuer="${KEYCLOAK_ISSUER:-http://localhost:8081/realms/taskflow}"
client="${KEYCLOAK_CLIENT_ID:-taskflow-spa}"
printf '{"issuer":"%s","clientId":"%s"}\n' "$issuer" "$client" > /usr/share/nginx/html/config.json
