package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
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
    private Optional<String> relayState = Optional.of("relayState");
    private String idpEntityId = "idpEntityId";
    private Optional<Boolean> forceAuthentication = Optional.of(true);

    public static FraudEventDetectedStateBuilder aFraudEventDetectedState() {
        return new FraudEventDetectedStateBuilder();
    }

    public FraudEventDetectedState build() {
        return new FraudEventDetectedState(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, relayState, sessionId, idpEntityId, forceAuthentication, false);
    }

}
