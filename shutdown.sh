#!/usr/bin/env bash

services=${@:-"config stub-event-sink policy saml-engine saml-proxy saml-soap-proxy"}

for service in $services; do
  pkill -9 -f "${service}.*.jar"
done

if docker ps | grep hub-redis >/dev/null ; then
    docker stop hub-redis
    docker rm hub-redis
fi

pushd ../verify-metadata > /dev/null
./kill-service.sh
popd > /dev/null

exit 0

