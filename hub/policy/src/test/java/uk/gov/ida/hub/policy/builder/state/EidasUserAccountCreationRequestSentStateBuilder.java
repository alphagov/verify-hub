package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState;

import java.net.URI;
import java.util.UUID;

import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class EidasUserAccountCreationRequestSentStateBuilder {

    private String requestId = UUID.randomUUID().toString();
    private String identityProviderEntityId = "idp entity id";
    private String requestIssuerId = "request issuer id";
    private String relayState = null;
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = aSessionId().build();

    public static EidasUserAccountCreationRequestSentStateBuilder anEidasUserAccountCreationRequestSentState() {
        return new EidasUserAccountCreationRequestSentStateBuilder();
    }

    public EidasUserAccountCreationRequestSentState build() {
        return new EidasUserAccountCreationRequestSentState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                identityProviderEntityId,
                relayState,
                levelOfAssurance,
                "matchingServiceEntityId",
                null);
    }

    public EidasUserAccountCreationRequestSentStateBuilder withRelayState(String relayState) {
        this.relayState = relayState;
        return this;
    }

    public EidasUserAccountCreationRequestSentStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public EidasUserAccountCreationRequestSentStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
