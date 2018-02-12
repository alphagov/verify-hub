package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorStateTransitional;

import java.net.URI;
import java.util.UUID;

import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class AuthnFailedErrorStateBuilder {

    private boolean transactionSupportsEidas = false;
    private String requestId = UUID.randomUUID().toString();
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceIndex = URI.create("/default-service-index");
    private String relayState = null;
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private String idpEntityId = "IDP Entity ID";
    private Boolean forceAuthentication = true;

    public static AuthnFailedErrorStateBuilder anAuthnFailedErrorState() {
        return new AuthnFailedErrorStateBuilder();
    }

    @Deprecated
    public AuthnFailedErrorStateTransitional buildTransitional() {
        return new AuthnFailedErrorStateTransitional(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceIndex,
                Optional.fromNullable(relayState),
                sessionId,
                idpEntityId,
                Optional.fromNullable(forceAuthentication),
                transactionSupportsEidas);
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

    public AuthnFailedErrorStateBuilder withTransactionSupportsEidas(boolean transactionSupportsEidas) {
        this.transactionSupportsEidas = transactionSupportsEidas;
        return this;
    }
}
