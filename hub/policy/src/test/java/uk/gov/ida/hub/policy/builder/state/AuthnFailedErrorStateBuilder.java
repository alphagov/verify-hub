package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;

import java.net.URI;
import java.util.UUID;

import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class AuthnFailedErrorStateBuilder {

    private boolean transactionSupportsEidas = false;
    private String requestId = UUID.randomUUID().toString();
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceIndex = URI.create("/default-service-index");
    private String relayState = null;
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private String idpEntityId = "IDP Entity ID";
    private Boolean forceAuthentication = true;

    public static AuthnFailedErrorStateBuilder anAuthnFailedErrorState() {
        return new AuthnFailedErrorStateBuilder();
    }

    public AuthnFailedErrorState build() {
        return new AuthnFailedErrorState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceIndex,
                relayState,
                sessionId,
                idpEntityId,
                forceAuthentication,
                transactionSupportsEidas);
    }

    public AuthnFailedErrorStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public AuthnFailedErrorStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public AuthnFailedErrorStateBuilder withTransactionSupportsEidas(boolean transactionSupportsEidas) {
        this.transactionSupportsEidas = transactionSupportsEidas;
        return this;
    }
}
