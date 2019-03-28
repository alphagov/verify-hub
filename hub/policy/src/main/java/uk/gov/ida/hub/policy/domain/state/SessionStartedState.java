package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.controller.SessionStartable;

import java.io.Serializable;
import java.net.URI;

public class SessionStartedState extends AbstractState implements SessionStartable, IdpSelectingState, EidasCountrySelectingState, ResponseProcessingState, Serializable {

    private static final long serialVersionUID = -2890730003642035273L;

    @JsonProperty
    private final String relayState;

    @JsonCreator
    public SessionStartedState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

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

    public SessionStartedState(SessionStartable state) {
        this(
                state.getRequestId(),
                state.getRelayState().get(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getForceAuthentication().get(),
                state.getSessionExpiryTimestamp(),
                state.getSessionId(),
                state.getTransactionSupportsEidas()
        );
    }

    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }
}
