package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;

public class SessionStartedState extends AbstractState implements IdpSelectingState, EidasCountrySelectingState, ResponseProcessingState, Serializable {

    private static final long serialVersionUID = -2890730003642035273L;

    @JsonProperty
    private final String relayState;

    @JsonCreator
    public SessionStartedState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

        super(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                forceAuthentication
        );

        this.relayState = relayState;
    }

    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }
}
