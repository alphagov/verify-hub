package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;

import java.net.URI;

public class FraudEventDetectedStateBuilder {

    private String requestId = "requestId";
    private String requestIssuerId = "requestId";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusHours(1);
    private URI assertionConsumerServiceUri = URI.create("assertionConsumerServiceUri");
    private SessionId sessionId = SessionId.createNewSessionId();
    private String relayState = "relayState";
    private String idpEntityId = "idpEntityId";
    private Boolean forceAuthentication = true;

    public static FraudEventDetectedStateBuilder aFraudEventDetectedState() {
        return new FraudEventDetectedStateBuilder();
    }

    public FraudEventDetectedState build() {
        return new FraudEventDetectedState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                relayState,
                sessionId,
                idpEntityId,
                forceAuthentication,
                false);
    }
}
