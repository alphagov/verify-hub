package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateTransitional;

public class SessionStartedStateBuilder {

    private String requestId = "requestId";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusDays(5);
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private boolean transactionSupportsEidas = false;

    public static SessionStartedStateBuilder aSessionStartedState() {
        return new SessionStartedStateBuilder();
    }

    public SessionStartedStateTransitional build() {
        return new SessionStartedStateTransitional(
                requestId,
                Optional.absent(),
                null,
                null,
                null,
                sessionExpiryTimestamp,
                sessionId,
                transactionSupportsEidas);
    }

    @Deprecated
    public SessionStartedState buildOld() {
        return new SessionStartedState(
                requestId,
                Optional.absent(),
                null,
                null,
                null,
                null,
                sessionExpiryTimestamp,
                sessionId,
                transactionSupportsEidas
        );
    }

    public SessionStartedStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public SessionStartedStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public SessionStartedStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public SessionStartedStateBuilder withTransactionSupportsEidas(boolean transactionSupportsEidas) {
        this.transactionSupportsEidas = transactionSupportsEidas;
        return this;
    }
}
