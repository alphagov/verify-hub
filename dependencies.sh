#! /bin/sh
gradle --daemon --recompile-scripts clean \
    configuration:dependencies \
    hub:config:dependencies \
    hub:policy:dependencies \
    hub:saml-engine:dependencies \
    hub:saml-proxy:dependencies \
    hub:saml-soap-proxy:dependencies \
    stub-event-sink:dependencies \
    > out/dependencies.txt
