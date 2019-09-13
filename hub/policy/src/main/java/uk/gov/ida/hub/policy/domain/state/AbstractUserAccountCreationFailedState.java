package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Optional;

public abstract class AbstractUserAccountCreationFailedState extends AbstractState implements ResponseProcessingState, ResponsePreparedState {

    private static final long serialVersionUID = 1388066257920569091L;

    @JsonProperty
    private String relayState;

    public AbstractUserAccountCreationFailedState(
        final String requestId,
        final String authnRequestIssuerEntityId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final String relayState,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final Boolean forceAuthentication) {

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

    @Override
    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }
}
