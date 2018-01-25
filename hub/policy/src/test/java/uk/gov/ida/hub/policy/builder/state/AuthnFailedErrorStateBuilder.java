package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Optional.absent;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class AuthnFailedErrorStateBuilder {

    private boolean transactionSupportsEidas = false;
    private String requestId = UUID.randomUUID().toString();
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceIndex = URI.create("/default-service-index");
    private Optional<String> relayState = absent();
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private String idpEntityId = "IDP Entity ID";
    private List<String> availableIdpEntityIds = new ArrayList<>();
    private Optional<Boolean> forceAuthentication = Optional.of(true);

    public static AuthnFailedErrorStateBuilder anAuthnFailedErrorState() {
        return new AuthnFailedErrorStateBuilder();
    }

    public AuthnFailedErrorStateBuilder withAvailableIdpEntityIds(List<String> availableIdpEntityIds) {
        this.availableIdpEntityIds = availableIdpEntityIds;
        return this;
    }

    public AuthnFailedErrorStateBuilder withTransactionSupportsEidas(boolean transactionSupportsEidas) {
        this.transactionSupportsEidas = transactionSupportsEidas;
        return this;
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
                availableIdpEntityIds,
                forceAuthentication,
                transactionSupportsEidas);
    }
}
