package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.List;

public class AuthnFailedErrorState extends AbstractState implements ResponsePreparedState, IdpSelectingState {

    private Optional<String> relayState;
    private String idpEntityId;
    private List<String> availableIdpEntityIds;
    private Optional<Boolean> forceAuthentication;

    public AuthnFailedErrorState(
            String requestId,
            String authnRequestIssuerEntityId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            Optional<String> relayState,
            SessionId sessionId,
            String idpEntityId,
            List<String> availableIdpEntityIds,
            Optional<Boolean> forceAuthentication,
            boolean transactionSupportsEidas) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
        this.idpEntityId = idpEntityId;
        this.availableIdpEntityIds = availableIdpEntityIds;
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

    @Override
    public List<String> getAvailableIdentityProviderEntityIds() {
        return availableIdpEntityIds;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }
}
