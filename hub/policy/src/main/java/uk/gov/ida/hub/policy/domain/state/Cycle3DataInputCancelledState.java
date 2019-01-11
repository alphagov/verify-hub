package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

public final class Cycle3DataInputCancelledState extends AbstractState implements ResponsePreparedState, Serializable {

    private static final long serialVersionUID = 9016732137997928472L;

    private final Optional<String> relayState;

    public Cycle3DataInputCancelledState(
        final String requestId,
        final DateTime sessionExpiryTimestamp,
        final Optional<String> relayState,
        final String requestIssuerId,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas) {

        super(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            null);

        this.relayState = relayState;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }
}
