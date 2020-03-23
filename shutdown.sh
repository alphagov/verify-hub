#!/usr/bin/env bash

services=${@:-"config stub-event-sink policy saml-engine saml-proxy saml-soap-proxy"}

for service in $services; do
  pkill -9 -f "${service}.jar"
done

if docker ps | grep hub-redis >/dev/null ; then
    docker stop hub-redis
    # Docker removes hub-redis when it stops
    # docker rm hub-redis
fi

exit 0

