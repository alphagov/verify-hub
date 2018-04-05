package uk.gov.ida.hub.policy.domain.state;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class FraudEventDetectedState extends AuthnFailedErrorState {

    private static final long serialVersionUID = -8284392098372162493L;

    public FraudEventDetectedState(
            String requestId,
            String requestIssuerId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            String relayState,
            SessionId sessionId,
            String idpEntityId,
            Boolean forceAuthentication,
            boolean transactionSupportsEidas) {

        super(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                relayState,
                sessionId,
                idpEntityId,
                forceAuthentication,
                transactionSupportsEidas);
    }
}
