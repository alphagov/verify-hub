#!/usr/bin/env bash

if test ! "$1" == "skip-build"; then
    ./gradlew clean build copyToLib -x test
fi

pushd ../verify-local-startup >/dev/null
    source lib/services.sh
    source config/env.sh

    start_service stub-event-sink ../ida-hub/hub/stub-event-sink configuration/hub/stub-event-sink.yml $EVENT_SINK_PORT
    start_service config ../ida-hub/hub/config configuration/hub/config.yml $CONFIG_PORT
    start_service policy ../ida-hub/hub/policy configuration/hub/policy.yml $POLICY_PORT
    start_service saml-engine ../ida-hub/hub/saml-engine configuration/hub/saml-engine.yml $SAML_ENGINE_PORT
    start_service saml-proxy ../ida-hub/hub/saml-proxy configuration/hub/saml-proxy.yml $SAML_PROXY_PORT
    start_service saml-soap-proxy ../ida-hub/hub/saml-soap-proxy configuration/hub/saml-soap-proxy.yml $SAML_SOAP_PROXY_PORT
    wait
popd >/dev/null