package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class AuthnFailedErrorStateTransitional extends AbstractState implements ResponsePreparedState, IdpSelectingStateTransitional {

    private Optional<String> relayState;
    private String idpEntityId;
    private Optional<Boolean> forceAuthentication;

    public AuthnFailedErrorStateTransitional(
            String requestId,
            String authnRequestIssuerEntityId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            Optional<String> relayState,
            SessionId sessionId,
            String idpEntityId,
            Optional<Boolean> forceAuthentication,
            boolean transactionSupportsEidas) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
        this.idpEntityId = idpEntityId;
        this.forceAuthentication = forceAuthentication;
    }

    @Override
    public Optional<Boolean> getForceAuthentication() {
        return forceAuthentication;
    }

    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }
}
