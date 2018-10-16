package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class AuthnFailedErrorState extends AbstractAuthnFailedErrorState implements IdpSelectingState {

    private static final long serialVersionUID = 8101005936409595481L;

    private String idpEntityId;
    private String relayState;

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

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, relayState, sessionId, transactionSupportsEidas, forceAuthentication);

        this.idpEntityId = idpEntityId;
        this.relayState = relayState;
    }

    // Keep this for now to make deserialization with the previous version of FraudEventDetectedState compatible
    // TODO: After this version has been released, remove the relayState property from this class
    @Override
    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }
}
