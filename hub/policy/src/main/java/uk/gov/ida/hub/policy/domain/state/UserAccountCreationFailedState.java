package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class UserAccountCreationFailedState extends AbstractUserAccountCreationFailedState {

    private static final long serialVersionUID = 3462121540778040610L;

    @JsonCreator
    public UserAccountCreationFailedState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("authnRequestIssuerEntityId") final String authnRequestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

        super(
            requestId,
            authnRequestIssuerEntityId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            relayState,
            sessionId,
            transactionSupportsEidas,
            null
        );
    }
}
