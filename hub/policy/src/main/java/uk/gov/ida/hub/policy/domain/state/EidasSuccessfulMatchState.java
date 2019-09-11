package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class EidasSuccessfulMatchState extends AbstractSuccessfulMatchState {

    private static final long serialVersionUID = 7677160699140073010L;

    @JsonCreator
    public EidasSuccessfulMatchState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("matchingServiceAssertion") final String matchingServiceAssertion,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("levelOfAssurance") final LevelOfAssurance levelOfAssurance,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

        super(
                requestId,
                sessionExpiryTimestamp,
                identityProviderEntityId,
                matchingServiceAssertion,
                relayState,
                requestIssuerId,
                assertionConsumerServiceUri,
                sessionId,
                levelOfAssurance,
                transactionSupportsEidas);
    }
}
