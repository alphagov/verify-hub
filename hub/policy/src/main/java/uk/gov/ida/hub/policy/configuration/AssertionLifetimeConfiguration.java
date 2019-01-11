package uk.gov.ida.hub.policy.configuration;

import io.dropwizard.util.Duration;

//TODO move into a shared library
public interface AssertionLifetimeConfiguration {
    Duration getAssertionLifetime();
}
