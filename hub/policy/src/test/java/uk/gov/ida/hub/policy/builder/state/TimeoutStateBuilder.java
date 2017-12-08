package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;

import java.net.URI;

public class TimeoutStateBuilder {
    private final boolean transactionSupportsEidas = false;
    private String requestId = "requestId";
    private String requestIssuerId = "requestId";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusHours(1);
    private URI assertionConsumerServiceUri = URI.create("assertionConsumerServiceUri");
    private SessionId sessionId = SessionId.createNewSessionId();

    public static TimeoutStateBuilder aTimeoutState() {
        return new TimeoutStateBuilder();
    }

    public TimeoutState build() {
        return new TimeoutState(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);
    }
}
