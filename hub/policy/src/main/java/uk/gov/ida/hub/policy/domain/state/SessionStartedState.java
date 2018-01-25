package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

public class SessionStartedState extends AbstractState implements IdpSelectingState, CountrySelectingState, ResponseProcessingState, Serializable {

    private final List<String> availableIdentityProviders;
    private final Optional<String> relayState;
    private final Optional<Boolean> forceAuthentication;

    public SessionStartedState(
            String requestId,
            Optional<String> relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri,
            Optional<Boolean> forceAuthentication,
            List<String> availableIdentityProviders,
            DateTime sessionExpiryTimestamp,
            SessionId sessionId,
            boolean transactionSupportsEidas) {

        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
        this.forceAuthentication = forceAuthentication;
        this.availableIdentityProviders = availableIdentityProviders;
    }

    public List<String> getAvailableIdentityProviderEntityIds() {
        return availableIdentityProviders;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public Optional<Boolean> getForceAuthentication() {
        return forceAuthentication;
    }
}
