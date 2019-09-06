package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Optional;

public abstract class AbstractAuthnFailedErrorState extends AbstractState implements ResponsePreparedState {

    private static final long serialVersionUID = 8101005936409595481L;

    @JsonProperty
    private String relayState;

    public AbstractAuthnFailedErrorState(
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
