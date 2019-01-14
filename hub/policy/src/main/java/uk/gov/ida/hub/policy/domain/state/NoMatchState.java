package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

public class NoMatchState extends AbstractState implements ResponseProcessingState, ResponsePreparedState, Serializable {

    private static final long serialVersionUID = 4256395503097984488L;

    private final String identityProviderEntityId;
    private final Optional<String> relayState;

    @JsonCreator
    public NoMatchState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("relayState") final Optional<String> relayState,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

        super(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                null
        );

        this.identityProviderEntityId = identityProviderEntityId;
        this.relayState = relayState;
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }
}
