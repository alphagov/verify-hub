#!/usr/bin/env bash

if test ! "$1" == "skip-build"; then
    ./gradlew --parallel --daemon clean build installDist -x test
fi

pushd ../ida-hub-acceptance-tests >/dev/null
    source scripts/services.sh
    source scripts/env.sh

    pushd ../verify-metadata >/dev/null
    $(pwd)/startup.sh
    popd >/dev/null

    # redis required for policy & saml-engine
    if ! docker ps | grep hub-redis >/dev/null
    then
      printf "$(tput setaf 3)Redis is required for policy and saml-engine, attempting to start redis using docker.\\n$(tput sgr0)"
      # deliberately avoiding the normal redis port in case there's another redis running
      docker run --rm -d -p 6378:6379 --name hub-redis redis >/dev/null
    fi

    start_service stub-event-sink ../verify-hub/hub/stub-event-sink configuration/hub/stub-event-sink.yml $EVENT_SINK_PORT
    start_service config ../verify-hub/hub/config configuration/hub/config.yml $CONFIG_PORT
    start_service policy ../verify-hub/hub/policy configuration/hub/policy.yml $POLICY_PORT
    start_service saml-engine ../verify-hub/hub/saml-engine configuration/hub/saml-engine.yml $SAML_ENGINE_PORT
    start_service saml-proxy ../verify-hub/hub/saml-proxy configuration/hub/saml-proxy.yml $SAML_PROXY_PORT
    start_service saml-soap-proxy ../verify-hub/hub/saml-soap-proxy configuration/hub/saml-soap-proxy.yml $SAML_SOAP_PROXY_PORT
    wait
popd >/dev/null
