package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.PausedRegistrationState;

import java.net.URI;

public class PausedRegistrationStateBuilder {

    private final String requestId = "some-request-id";
    private final String requestIssuerId = "some-request-issuer-id";
    private final DateTime sessionExpiryTimestamp = new DateTime(1988, 1, 1, 0, 0, DateTimeZone.UTC);
    private final URI assertionConsumerServiceUri = URI.create("urn:some:assertion:consumer:service");
    private final SessionId sessionId = new SessionId("some-session-id");
    private final boolean transactionSupportsEidas = true;
    private final String relayState = "some-relay-state";

    public static PausedRegistrationStateBuilder aPausedRegistrationState() {
        return new PausedRegistrationStateBuilder();
    }

    public PausedRegistrationState build() {
        return new PausedRegistrationState(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            Optional.of(relayState)
        );
    }
}
