package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class UserAccountCreationFailedState extends AbstractUserAccountCreationFailedState {

    private static final long serialVersionUID = 3462121540778040610L;

    private final Optional<String> relayState;

    public UserAccountCreationFailedState(
        String requestId,
        String authnRequestIssuerEntityId,
        DateTime sessionExpiryTimestamp,
        URI assertionConsumerServiceUri,
        Optional<String> relayState,
        SessionId sessionId,
        boolean transactionSupportsEidas) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, relayState, sessionId, transactionSupportsEidas);

        this.relayState = relayState;
    }

    // Keep this for now to make deserialization with the previous version of UserAccountCreationFailedState compatible
    // TODO: After this version has been released, remove the relayState property from this class
    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }
}
