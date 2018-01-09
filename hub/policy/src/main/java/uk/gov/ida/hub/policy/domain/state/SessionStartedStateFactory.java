package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.inject.Inject;
import java.net.URI;

public class SessionStartedStateFactory {

    @Inject
    public SessionStartedStateFactory() {}

    public SessionStartedState build(
            String authnRequestId,
            URI assertionConsumerServiceUri,
            String requestIssuerId,
            Optional<String> relayState,
            Optional<Boolean> forceAuthentication,
            DateTime sessionExpiryTimestamp,
            SessionId sessionId,
            boolean transactionSupportsEidas) {

        return new SessionStartedState(
                authnRequestId,
                relayState,
                requestIssuerId,
                assertionConsumerServiceUri,
                forceAuthentication,
                sessionExpiryTimestamp,
                sessionId,
                transactionSupportsEidas
        );
    }
}
