package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;

public class Cycle3DataInputCancelledState extends AbstractState implements ResponsePreparedState, Serializable {

    private static final long serialVersionUID = 9016732137997928472L;

    @JsonProperty
    private final String relayState;

    @JsonCreator
    public Cycle3DataInputCancelledState(
        @JsonProperty("requestId") final String requestId,
        @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
        @JsonProperty("relayState") final String relayState,
        @JsonProperty("requestIssuerId") final String requestIssuerId,
        @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
        @JsonProperty("sessionId") final SessionId sessionId,
        @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

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
        return Optional.ofNullable(relayState);
    }
}
