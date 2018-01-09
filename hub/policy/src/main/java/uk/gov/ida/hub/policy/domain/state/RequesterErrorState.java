package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class RequesterErrorState extends AbstractState implements IdpSelectingState, ResponsePreparedState {

    private Optional<String> relayState;
    private Optional<Boolean> forceAuthentication;

    public RequesterErrorState(
            String requestId,
            String authnRequestIssuerEntityId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            Optional<String> relayState,
            SessionId sessionId,
            Optional<Boolean> forceAuthentication,
            boolean transactionSupportsEidas) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
        this.forceAuthentication = forceAuthentication;
    }

    @Override
    public Optional<Boolean> getForceAuthentication() {
        return forceAuthentication;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }
}
