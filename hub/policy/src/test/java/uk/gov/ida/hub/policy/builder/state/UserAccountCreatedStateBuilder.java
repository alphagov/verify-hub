package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;

import java.net.URI;

public class UserAccountCreatedStateBuilder {

    private String requestId = "request-id";
    private URI assertionConsumerServiceUri = URI.create("http://assertionconsumeruri");
    private String requestIssuerId = "request issuer id";
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private String identityProviderEntityId = "identity-provider-id";
    private String matchingServiceAssertion = "aPassthroughAssertion().buildMatchingServiceAssertion()";
    private String relayState = null;
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private boolean registering = false;

    public static UserAccountCreatedStateBuilder aUserAccountCreatedState() {
        return new UserAccountCreatedStateBuilder();
    }

    public UserAccountCreatedState build() {
        return new UserAccountCreatedState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                identityProviderEntityId,
                matchingServiceAssertion,
                relayState,
                levelOfAssurance,
                registering);
    }

    public UserAccountCreatedStateBuilder withIdentityProviderEntityId(final String identityProviderEntityId) {
        this.identityProviderEntityId = identityProviderEntityId;
        return this;
    }

    public UserAccountCreatedStateBuilder withRelayState(String relayState) {
        this.relayState = relayState;
        return this;
    }

    public UserAccountCreatedStateBuilder withRegistering(boolean registering) {
        this.registering = registering;
        return this;
    }

    public UserAccountCreatedStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public UserAccountCreatedStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public UserAccountCreatedStateBuilder withMatchingServiceAssertion(String matchingServiceAssertion) {
        this.matchingServiceAssertion = matchingServiceAssertion;
        return this;
    }

    public UserAccountCreatedStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public UserAccountCreatedStateBuilder withAssertionConsumerServiceUri(URI assertionConsumerServiceUri) {
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        return this;
    }

    public UserAccountCreatedStateBuilder withRequestIssuerId(String requestIssuerId) {
        this.requestIssuerId = requestIssuerId;
        return this;
    }
}
