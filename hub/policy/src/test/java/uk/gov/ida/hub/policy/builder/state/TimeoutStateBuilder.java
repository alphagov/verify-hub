package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;

import java.net.URI;

public class TimeoutStateBuilder {
    private final boolean transactionSupportsEidas = false;
    private String requestId = "requestId";
    private String requestIssuerId = "requestId";
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusHours(1);
    private URI assertionConsumerServiceUri = URI.create("assertionConsumerServiceUri");
    private SessionId sessionId = SessionId.createNewSessionId();

    public static TimeoutStateBuilder aTimeoutState() {
        return new TimeoutStateBuilder();
    }

    public TimeoutStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public TimeoutState build() {
        return new TimeoutState(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);
    }
}
