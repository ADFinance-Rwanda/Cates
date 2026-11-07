#!/bin/sh
# Render injects DATABASE_URL as postgres:// and PORT as the public port.
# Compose sets SPRING_DATASOURCE_URL and SERVER_PORT and does not set those.
set -eu

urldecode() {
  printf '%s' "$1" | awk '
    function hex(h, i, c, n) {
      n = 0
      for (i = 1; i <= length(h); i++) {
        c = index("0123456789ABCDEF", toupper(substr(h, i, 1)))
        n = n * 16 + c - 1
      }
      return sprintf("%c", n)
    }
    {
      s = $0
      gsub(/\+/, " ", s)
      while (match(s, /%[0-9A-Fa-f][0-9A-Fa-f]/)) {
        pre = substr(s, 1, RSTART - 1)
        code = substr(s, RSTART + 1, 2)
        s = pre hex(code) substr(s, RSTART + RLENGTH)
      }
      printf "%s", s
    }
  '
}

if [ -z "${SERVER_PORT:-}" ] && [ -n "${PORT:-}" ]; then
  export SERVER_PORT="$PORT"
fi

if [ -n "${DATABASE_URL:-}" ]; then
  without_scheme=${DATABASE_URL#*://}
  userpass=${without_scheme%%@*}
  hostpart=${without_scheme#*@}
  db_user=$(urldecode "${userpass%%:*}")
  db_pass=$(urldecode "${userpass#*:}")
  case "$hostpart" in
    *:*)
      db_host=${hostpart%%:*}
      port_and_db=${hostpart#*:}
      db_port=${port_and_db%%/*}
      db_name=${port_and_db#*/}
      ;;
    *)
      db_host=${hostpart%%/*}
      db_port=5432
      db_name=${hostpart#*/}
      ;;
  esac
  db_name=${db_name%%\?*}
  jdbc="jdbc:postgresql://${db_host}:${db_port}/${db_name}"
  case "$db_host" in
    *.render.com) jdbc="${jdbc}?sslmode=require" ;;
  esac
  export SPRING_DATASOURCE_URL="$jdbc"
  export SPRING_DATASOURCE_USERNAME="$db_user"
  export SPRING_DATASOURCE_PASSWORD="$db_pass"
fi

if [ -n "${REDIS_URL:-}" ]; then
  export SPRING_DATA_REDIS_URL="$REDIS_URL"
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar
