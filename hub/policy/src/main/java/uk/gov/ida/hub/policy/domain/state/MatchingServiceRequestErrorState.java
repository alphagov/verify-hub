package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;

public class MatchingServiceRequestErrorState extends AbstractState implements ResponseProcessingState, Serializable {

    private static final long serialVersionUID = 4748254124761879612L;

    @JsonProperty
    private final String identityProviderEntityId;
    @JsonProperty
    private String relayState;

    @JsonCreator
    public MatchingServiceRequestErrorState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("relayState") final String relayState,
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

    @Override
    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }
}
