package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;

import java.net.URI;

public class EidasSuccessfulMatchStateBuilder {

    private String requestId = "request-id";
    private String identityProviderEntityId = "country-entity-id";
    private String matchingServiceAssertion = "aPassthroughAssertion().buildMatchingServiceAssertion()";
    private URI assertionConsumerServiceUri = URI.create("http://assertionconsumeruri");
    private Optional<String> relayState = Optional.fromNullable("relay state");
    private String requestIssuerId = "request issuer id";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId getSessionId = SessionIdBuilder.aSessionId().build();
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private boolean transactionSupportsEidas = true;

    public static EidasSuccessfulMatchStateBuilder aEidasSuccessfulMatchState() {
        return new EidasSuccessfulMatchStateBuilder();
    }

    public EidasSuccessfulMatchState build() {
        return new EidasSuccessfulMatchState(
                requestId,
                sessionExpiryTimestamp,
                identityProviderEntityId,
                matchingServiceAssertion,
                relayState,
                requestIssuerId,
                assertionConsumerServiceUri,
                getSessionId,
                levelOfAssurance,
                transactionSupportsEidas);
    }

    public EidasSuccessfulMatchStateBuilder withSessionId(SessionId sessionId) {
        this.getSessionId = sessionId;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withIdentityProviderEntityId(String identityProviderEntityId) {
        this.identityProviderEntityId = identityProviderEntityId;
        return this;
    }

    public EidasSuccessfulMatchStateBuilder withRequestIssuerId(String requestIssuerId) {
        this.requestIssuerId = requestIssuerId;
        return this;
    }
}
