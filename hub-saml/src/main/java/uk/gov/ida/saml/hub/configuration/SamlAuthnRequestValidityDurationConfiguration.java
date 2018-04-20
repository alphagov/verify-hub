package uk.gov.ida.saml.hub.configuration;

import io.dropwizard.util.Duration;

public interface SamlAuthnRequestValidityDurationConfiguration {
    Duration getAuthnRequestValidityDuration();
}
