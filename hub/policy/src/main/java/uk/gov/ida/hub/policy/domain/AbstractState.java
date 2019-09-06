package uk.gov.ida.hub.policy.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;

public abstract class AbstractState implements State, Serializable, ErrorResponsePreparedState {

    private static final long serialVersionUID = -4735026295130074234L;

    @JsonProperty
    private final String requestId;
    @JsonProperty
    private final String requestIssuerEntityId;
    @JsonProperty
    private final DateTime sessionExpiryTimestamp;
    @JsonProperty
    private final URI assertionConsumerServiceUri;
    @JsonProperty
    private final SessionId sessionId;
    @JsonProperty
    private final boolean transactionSupportsEidas;
    @JsonProperty
    private final Boolean forceAuthentication;

    protected AbstractState(
        final String requestId,
        final String requestIssuerEntityId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final Boolean forceAuthentication) {

        this.requestId = requestId;
        this.requestIssuerEntityId = requestIssuerEntityId;
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        this.sessionId = sessionId;
        this.transactionSupportsEidas = transactionSupportsEidas;
        this.forceAuthentication = forceAuthentication;
    }

    @Override
    public final String getRequestId() {
        return requestId;
    }

    @Override
    public final SessionId getSessionId(){
        return sessionId;
    }

    @Override
    public final String getRequestIssuerEntityId() {
        return requestIssuerEntityId;
    }

    @Override
    public DateTime getSessionExpiryTimestamp() {
        return sessionExpiryTimestamp;
    }

    @Override
    public final URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    @Override
    public final void doNotDirectlyImplementThisInterface() {}

    @Override
    public boolean getTransactionSupportsEidas() {
        return transactionSupportsEidas;
    }

    @Override
    public Optional<Boolean> getForceAuthentication() { return Optional.ofNullable(forceAuthentication); }
}
