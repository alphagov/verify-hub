package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class AuthnFailedErrorState extends AbstractState implements IdpSelectingState, ResponsePreparedState {

    private String relayState;
    private String idpEntityId;
    private Boolean forceAuthentication;

    public AuthnFailedErrorState(
            String requestId,
            String authnRequestIssuerEntityId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            String relayState,
            SessionId sessionId,
            String idpEntityId,
            Boolean forceAuthentication,
            boolean transactionSupportsEidas) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
        this.idpEntityId = idpEntityId;
        this.forceAuthentication = forceAuthentication;
    }

    @Override
    public Optional<Boolean> getForceAuthentication() {
        return Optional.fromNullable(forceAuthentication);
    }

    @Override
    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }
}
