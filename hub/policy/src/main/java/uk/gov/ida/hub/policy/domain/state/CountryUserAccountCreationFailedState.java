package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class CountryUserAccountCreationFailedState extends AbstractUserAccountCreationFailedState implements EidasUnsuccessfulJourneyState {

    private static final long serialVersionUID = -2561859918430555052L;

    public CountryUserAccountCreationFailedState(
        String requestId,
        String authnRequestIssuerEntityId,
        DateTime sessionExpiryTimestamp,
        URI assertionConsumerServiceUri,
        Optional<String> relayState,
        SessionId sessionId) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, relayState, sessionId, true);
    }
}
