package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public abstract class AbstractAuthnFailedErrorState extends AbstractState implements ResponsePreparedState {

    private static final long serialVersionUID = 8101005936409595481L;

    private String relayState;

    public AbstractAuthnFailedErrorState(
            String requestId,
            String authnRequestIssuerEntityId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            String relayState,
            SessionId sessionId,
            boolean transactionSupportsEidas) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
    }

    @Override
    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }
}
