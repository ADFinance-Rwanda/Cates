FROM quay.io/keycloak/keycloak:26.4.0
COPY realm-export.json /opt/keycloak/data/import/taskflow-realm.json
COPY themes/taskflow /opt/keycloak/themes/taskflow
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev", "--http-port=10000", "--import-realm"]
