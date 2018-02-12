package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class SessionStartedState extends AbstractState implements IdpSelectingState, CountrySelectingState, ResponseProcessingState, Serializable {

    private final String relayState;
    private final Boolean forceAuthentication;

    public SessionStartedState(
            String requestId,
            String relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri,
            Boolean forceAuthentication,
            DateTime sessionExpiryTimestamp,
            SessionId sessionId,
            boolean transactionSupportsEidas) {

        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
        this.forceAuthentication = forceAuthentication;
    }

    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }

    public Optional<Boolean> getForceAuthentication() {
        return Optional.fromNullable(forceAuthentication);
    }
}
