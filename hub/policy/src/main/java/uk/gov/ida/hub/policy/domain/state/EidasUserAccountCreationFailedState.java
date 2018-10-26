package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class EidasUserAccountCreationFailedState extends AbstractUserAccountCreationFailedState implements RestartJourneyState {

    private static final long serialVersionUID = -2561859918430555052L;

    public EidasUserAccountCreationFailedState(
        String requestId,
        String authnRequestIssuerEntityId,
        DateTime sessionExpiryTimestamp,
        URI assertionConsumerServiceUri,
        Optional<String> relayState,
        SessionId sessionId,
        Boolean forceAuthentication) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, relayState, sessionId, true, forceAuthentication);

    }
}
