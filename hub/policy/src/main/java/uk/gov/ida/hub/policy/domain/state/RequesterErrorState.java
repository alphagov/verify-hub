package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class RequesterErrorState extends AbstractState implements IdpSelectingState, ResponsePreparedState {

    private static final long serialVersionUID = -1738587884705979267L;

    private String relayState;

    public RequesterErrorState(
            String requestId,
            String authnRequestIssuerEntityId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            String relayState,
            SessionId sessionId,
            Boolean forceAuthentication,
            boolean transactionSupportsEidas) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas, forceAuthentication);

        this.relayState = relayState;
    }

    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }
}
