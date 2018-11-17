#!/usr/bin/env bash

start() {
  if [ -r /etc/default/verify ]; then
    . /etc/default/verify
  fi
  if [ -r /etc/default/config ]; then
    . /etc/default/config
  fi

  export JAVA_OPTS="-Dservice.name=config \
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
  exec /ida/config/bin/config \
    server /ida/config/config.yml \
    1>> /var/log/ida/config.console-log 2>&1
}

stop() {
  if [ -r /etc/default/verify ]; then
    . /etc/default/verify
  fi
  if [ -r /etc/default/config ]; then
    . /etc/default/config
  fi

  #Take out of service from haproxy
  curl -X POST http://localhost:50241/tasks/set-service-unavailable
  sleep ${HAPROXY_SLEEP_TIME:-6}
}

case $1 in
  start|stop) "$1" ;;
esac
