package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class RequesterErrorState extends AbstractState implements IdpSelectingState, ResponsePreparedState {

    private static final long serialVersionUID = -1738587884705979267L;

    @JsonProperty
    private String relayState;

    @JsonCreator
    public RequesterErrorState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("authnRequestIssuerEntityId") final String authnRequestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

        super(
                requestId,
                authnRequestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                forceAuthentication
        );

        this.relayState = relayState;
    }

    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }
}
