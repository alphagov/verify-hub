package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class SessionStartedStateTransitional extends AbstractState implements IdpSelectingStateTransitional, CountrySelectingState, ResponseProcessingState, Serializable {

    private final Optional<String> relayState;
    private final Optional<Boolean> forceAuthentication;

    public SessionStartedStateTransitional(
            String requestId,
            Optional<String> relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri,
            Optional<Boolean> forceAuthentication,
            DateTime sessionExpiryTimestamp,
            SessionId sessionId,
            boolean transactionSupportsEidas) {

        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
        this.forceAuthentication = forceAuthentication;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public Optional<Boolean> getForceAuthentication() {
        return forceAuthentication;
    }
}
