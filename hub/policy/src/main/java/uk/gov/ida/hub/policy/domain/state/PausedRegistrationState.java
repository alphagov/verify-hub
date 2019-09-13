package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import java.net.URI;
import java.util.Optional;

public class PausedRegistrationState extends AbstractState implements State {

    private static final long serialVersionUID = 8208525157755502287L;

    @JsonProperty
    private String relayState;

    @JsonCreator
    public PausedRegistrationState(
           @JsonProperty("requestId") final String requestId,
           @JsonProperty("requestIssuerId") final String requestIssuerId,
           @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
           @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
           @JsonProperty("sessionId") final SessionId sessionId,
           @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
           @JsonProperty("relayState") final String relayState) {

        super(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                null
        );

        this.relayState = relayState;
    }

    @Override
    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }
}
