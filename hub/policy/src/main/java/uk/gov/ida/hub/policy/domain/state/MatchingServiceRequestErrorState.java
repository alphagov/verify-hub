package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class MatchingServiceRequestErrorState extends AbstractState implements ResponseProcessingState, Serializable {

    private final String identityProviderEntityId;
    private Optional<String> relayState;

    public MatchingServiceRequestErrorState(
            final String requestId,
            final String requestIssuerId,
            final DateTime sessionExpiryTimestamp,
            final URI assertionConsumerServiceUri,
            final String identityProviderEntityId,
            final Optional<String> relayState,
            SessionId sessionId,
            boolean transactionSupportsEidas) {

        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.identityProviderEntityId = identityProviderEntityId;
        this.relayState = relayState;
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
    }

    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }
}
