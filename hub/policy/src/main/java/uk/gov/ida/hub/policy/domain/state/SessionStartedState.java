package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class SessionStartedState extends AbstractState implements IdpSelectingState, EidasCountrySelectingState, ResponseProcessingState, Serializable {

    private static final long serialVersionUID = -2890730003642035273L;

    private final String relayState;

    public SessionStartedState(
        String requestId,
        String relayState,
        String requestIssuerId,
        URI assertionConsumerServiceUri,
        Boolean forceAuthentication,
        DateTime sessionExpiryTimestamp,
        SessionId sessionId,
        boolean transactionSupportsEidas) {

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
        return Optional.fromNullable(relayState);
    }
}
