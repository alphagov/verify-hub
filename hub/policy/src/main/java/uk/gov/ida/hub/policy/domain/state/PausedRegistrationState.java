package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import java.net.URI;

public class PausedRegistrationState extends AbstractState implements State {

    private static final long serialVersionUID = 8208525157755502287L;

    private Optional<String> relayState;

    public PausedRegistrationState(String requestId,
                                   String requestIssuerId,
                                   DateTime sessionExpiryTimestamp,
                                   URI assertionConsumerServiceUri,
                                   SessionId sessionId,
                                   boolean transactionSupportsEidas,
                                   Optional<String> relayState) {
        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas, null);
        this.relayState = relayState;
    }

    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }
}
