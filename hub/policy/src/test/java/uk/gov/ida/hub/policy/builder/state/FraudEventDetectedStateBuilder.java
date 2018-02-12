package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedStateTransitional;

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

    @Deprecated
    public FraudEventDetectedStateTransitional buildTransitional() {
        return new FraudEventDetectedStateTransitional(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                Optional.fromNullable(relayState),
                sessionId,
                idpEntityId,
                Optional.fromNullable(forceAuthentication),
                false);
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
