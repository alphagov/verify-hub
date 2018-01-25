package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class FraudEventDetectedState extends AuthnFailedErrorState {

    public FraudEventDetectedState(
            String requestId,
            String requestIssuerId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            Optional<String> relayState,
            SessionId sessionId,
            String idpEntityId,
            Optional<Boolean> forceAuthentication,
            boolean transactionSupportsEidas) {

        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, relayState, sessionId, idpEntityId, forceAuthentication, transactionSupportsEidas);
    }
}
