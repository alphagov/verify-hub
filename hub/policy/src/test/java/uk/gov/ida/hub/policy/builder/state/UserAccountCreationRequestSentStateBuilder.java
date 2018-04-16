package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;

import java.net.URI;
import java.util.UUID;

import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class UserAccountCreationRequestSentStateBuilder {

    private String requestId = UUID.randomUUID().toString();
    private String identityProviderEntityId = "idp entity id";
    private String requestIssuerId = "request issuer id";
    private String relayState = null;
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_1;
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private boolean transactionSupportsEidas = false;
    private boolean registering = false;

    public static UserAccountCreationRequestSentStateBuilder aUserAccountCreationRequestSentState() {
        return new UserAccountCreationRequestSentStateBuilder();
    }

    public UserAccountCreationRequestSentState build() {
        return new UserAccountCreationRequestSentState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                identityProviderEntityId,
                relayState,
                levelOfAssurance,
                registering,
                "matchingServiceEntityId"
        );
    }

    public UserAccountCreationRequestSentStateBuilder withRelayState(String relayState) {
        this.relayState = relayState;
        return this;
    }

    public UserAccountCreationRequestSentStateBuilder withRegistering(boolean registering) {
        this.registering = registering;
        return this;
    }

    public UserAccountCreationRequestSentStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public UserAccountCreationRequestSentStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
