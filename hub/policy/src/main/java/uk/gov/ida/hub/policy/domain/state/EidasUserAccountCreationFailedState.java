package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class EidasUserAccountCreationFailedState extends AbstractUserAccountCreationFailedState implements RestartJourneyState {

    private static final long serialVersionUID = -2561859918430555052L;

    @JsonCreator
    public EidasUserAccountCreationFailedState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("authnRequestIssuerEntityId") final String authnRequestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("relayState") final Optional<String> relayState,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, relayState, sessionId, true, forceAuthentication);

    }
}
