#!/usr/bin/env bash

start() {
  if [ -r /etc/default/verify ]; then
    . /etc/default/verify
  fi
  if [ -r /etc/default/saml-engine ]; then
    . /etc/default/saml-engine
  fi

  export JAVA_OPTS="-Dservice.name=saml-engine \
                      -XX:HeapDumpPath=/var/log/ida/debug \
                      -XX:+HeapDumpOnOutOfMemoryError \
                      -Dhttp.proxyHost=\"${PROXY_HOST:-}\" \
                      -Dhttp.proxyPort=\"${PROXY_PORT:-}\" \
                      -Dhttps.proxyHost=\"${PROXY_HOST:-}\" \
                      -Dhttps.proxyPort=\"${PROXY_PORT:-}\" \
                      -Dhttp.nonProxyHosts=\"${NON_PROXY_HOSTS:-}\" \
                      -Dnetworkaddress.cache.ttl=5 \
                      -Dnetworkaddress.cache.negative.ttl=5 \
                      $EXTRA_JAVA_OPTS"

  # JAVA_HOME is set wrong, but the `java` on the path is correct - unsetting this uses /usr/bin/java
  unset JAVA_HOME
  exec start-stop-daemon \
    --start \
    -c saml-engine \
    --make-pidfile \
    --pidfile "/var/run/saml-engine.pidfile" \
    --exec /ida/saml-engine/bin/saml-engine \
    -- \
    server /ida/saml-engine/saml-engine.yml \
    1>> /var/log/ida/saml-engine.console-log 2>&1 \
    4<${PKI_PRIMARY_PRIVATE_ENCRYPTION_KEY_PATH:-/ida/apps-home/keys/hub_encryption_primary.pk8} \
    5<${PKI_SECONDARY_PRIVATE_ENCRYPTION_KEY_PATH:-/ida/apps-home/keys/hub_encryption_secondary.pk8} \
    6<${PKI_PRIVATE_SIGNING_KEY_PATH:-/ida/apps-home/keys/hub_signing.pk8}
}

stop() {
  if [ -r /etc/default/verify ]; then
    . /etc/default/verify
  fi
  if [ -r /etc/default/saml-engine ]; then
    . /etc/default/saml-engine
  fi

  #Take out of service from haproxy
  curl -X POST http://localhost:50121/tasks/set-service-unavailable
  sleep ${HAPROXY_SLEEP_TIME:-6}
}

case $1 in
  start|stop) "$1" ;;
esac
