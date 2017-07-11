package uk.gov.ida.saml.hub.configuration;

import io.dropwizard.util.Duration;

public interface SamlDuplicateRequestValidationConfiguration {
    Duration getAuthnRequestIdExpirationDuration();
}
