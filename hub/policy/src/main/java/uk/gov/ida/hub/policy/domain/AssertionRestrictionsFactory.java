package uk.gov.ida.hub.policy.domain;

import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.AssertionLifetimeConfiguration;

import javax.inject.Inject;

public class AssertionRestrictionsFactory {

    private final Duration assertionLifetime;

    @Inject
    public AssertionRestrictionsFactory(AssertionLifetimeConfiguration assertionTimeoutConfig) {
        assertionLifetime = assertionTimeoutConfig.getAssertionLifetime();
    }

    public DateTime getAssertionExpiry() {
        return DateTime.now().plus(assertionLifetime.toMilliseconds());
    }
}
