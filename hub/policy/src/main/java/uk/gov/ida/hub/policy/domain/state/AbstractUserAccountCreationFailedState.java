package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public abstract class AbstractUserAccountCreationFailedState extends AbstractState implements ResponseProcessingState, ResponsePreparedState {

    private static final long serialVersionUID = 1388066257920569091L;

    @JsonProperty
    private Optional<String> relayState;

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

        this.relayState = Optional.fromNullable(relayState);
    }

    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }
}
