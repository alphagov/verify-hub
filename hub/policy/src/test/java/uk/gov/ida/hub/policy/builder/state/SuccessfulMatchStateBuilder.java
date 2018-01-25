package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;

import java.net.URI;

public class SuccessfulMatchStateBuilder {

    private String requestId = "request-id";
    private String identityProviderEntityId = "idp-entity-id";
    private String matchingServiceAssertion = "aPassthroughAssertion().buildMatchingServiceAssertion()";
    private URI assertionConsumerServiceUri = URI.create("http://assertionconsumeruri");
    private Optional<String> relayState = Optional.fromNullable("relay state");
    private String requestIssuerId = "request issuer id";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId getSessionId = SessionIdBuilder.aSessionId().build();
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private boolean isRegistering = false;
    private boolean transactionSupportsEidas = false;

    public static SuccessfulMatchStateBuilder aSuccessfulMatchState() {
        return new SuccessfulMatchStateBuilder();
    }

    public SuccessfulMatchState build() {
        return new SuccessfulMatchState(
                requestId,
                sessionExpiryTimestamp,
                identityProviderEntityId,
                matchingServiceAssertion,
                relayState,
                requestIssuerId,
                assertionConsumerServiceUri,
                getSessionId,
                levelOfAssurance,
                isRegistering,
                transactionSupportsEidas);
    }

    public SuccessfulMatchStateBuilder withSessionId(SessionId sessionId) {
        this.getSessionId = sessionId;
        return this;
    }

    public SuccessfulMatchStateBuilder withIdentityProviderEntityId(String identityProviderEntityId) {
        this.identityProviderEntityId = identityProviderEntityId;
        return this;
    }

    public SuccessfulMatchStateBuilder withRequestIssuerEntityId(String requestIssuerEntityId) {
        this.requestIssuerId = requestIssuerEntityId;
        return this;
    }

    public SuccessfulMatchStateBuilder withRegistering(boolean isRegistering) {
        this.isRegistering = isRegistering;
        return this;
    }
}
