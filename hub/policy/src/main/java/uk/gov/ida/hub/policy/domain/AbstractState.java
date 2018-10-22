package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;

import java.io.Serializable;
import java.net.URI;

public abstract class AbstractState implements State, Serializable, ErrorResponsePreparedState {

    private static final long serialVersionUID = -4735026295130074234L;

    private final String requestId;
    private final String requestIssuerEntityId;
    private final DateTime sessionExpiryTimestamp;
    private final URI assertionConsumerServiceUri;
    private final SessionId sessionId;
    private final boolean transactionSupportsEidas;
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

    public Optional<Boolean> getForceAuthentication() {
        return Optional.fromNullable(forceAuthentication);
    }
}
