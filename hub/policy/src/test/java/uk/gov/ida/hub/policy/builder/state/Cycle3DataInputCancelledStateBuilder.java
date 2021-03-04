package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;

import java.net.URI;
import java.util.UUID;

public class Cycle3DataInputCancelledStateBuilder {

    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private String requestId = UUID.randomUUID().toString();

    public static Cycle3DataInputCancelledStateBuilder aCycle3DataInputCancelledState() {
        return new Cycle3DataInputCancelledStateBuilder();
    }

    public Cycle3DataInputCancelledState build() {
        return new Cycle3DataInputCancelledState(requestId, sessionExpiryTimestamp, null, requestIssuerId, assertionConsumerServiceUri, sessionId);
    }

    public Cycle3DataInputCancelledStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Cycle3DataInputCancelledStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
