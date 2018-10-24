package uk.gov.ida.hub.policy.domain.state;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class AuthnFailedErrorState extends AbstractAuthnFailedErrorState implements IdpSelectingState {

    private static final long serialVersionUID = 8101005936409595481L;

    private String idpEntityId;

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
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }
}
