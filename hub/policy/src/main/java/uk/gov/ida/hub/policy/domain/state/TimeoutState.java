package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class TimeoutState extends AbstractState implements Serializable {

    private static final long serialVersionUID = -4390191044338229404L;

    public TimeoutState(
            String requestId,
            String requestIssuerId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            SessionId sessionId,
            boolean transactionSupportsEidas) {

        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas, null);
    }

    @Override
    public Optional<String> getRelayState() {
        return Optional.absent();
    }
}
