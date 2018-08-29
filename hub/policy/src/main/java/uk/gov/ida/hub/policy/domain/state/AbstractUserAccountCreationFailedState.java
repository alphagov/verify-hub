package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public abstract class AbstractUserAccountCreationFailedState extends AbstractState implements ResponseProcessingState, ResponsePreparedState {

    private static final long serialVersionUID = 1388066257920569091L;

    private Optional<String> relayState;

    public AbstractUserAccountCreationFailedState(
        String requestId,
        String authnRequestIssuerEntityId,
        DateTime sessionExpiryTimestamp,
        URI assertionConsumerServiceUri,
        Optional<String> relayState,
        SessionId sessionId,
        boolean transactionSupportsEidas) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
    }

    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }
}
