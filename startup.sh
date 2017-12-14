#!/usr/bin/env bash

if test ! "$1" == "skip-build"; then
    ./gradlew clean build copyToLib -x test
fi

pushd ../verify-local-startup >/dev/null
    source lib/services.sh
    source config/env.sh

    start_service stub-event-sink ../verify-hub/hub/stub-event-sink configuration/hub/stub-event-sink.yml $EVENT_SINK_PORT
    start_service config ../verify-hub/hub/config configuration/hub/config.yml $CONFIG_PORT
    start_service policy ../verify-hub/hub/policy configuration/hub/policy.yml $POLICY_PORT
    start_service saml-engine ../verify-hub/hub/saml-engine configuration/hub/saml-engine.yml $SAML_ENGINE_PORT
    start_service saml-proxy ../verify-hub/hub/saml-proxy configuration/hub/saml-proxy.yml $SAML_PROXY_PORT
    start_service saml-soap-proxy ../verify-hub/hub/saml-soap-proxy configuration/hub/saml-soap-proxy.yml $SAML_SOAP_PROXY_PORT
    wait
popd >/dev/null
