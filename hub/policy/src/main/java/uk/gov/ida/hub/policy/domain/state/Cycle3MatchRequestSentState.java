package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class Cycle3MatchRequestSentState extends Cycle0And1MatchRequestSentState implements Serializable {

    private static final long serialVersionUID = 7239719376154151711L;

    @JsonCreator
    public Cycle3MatchRequestSentState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerEntityId") final String requestIssuerEntityId,
            @JsonProperty("sessionExpiryTime") final DateTime sessionExpiryTime,
            @JsonProperty("assertionConsumerServiceIndex") final URI assertionConsumerServiceIndex,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("idpLevelOfAssurance") final LevelOfAssurance idpLevelOfAssurance,
            @JsonProperty("registering") final boolean registering,
            @JsonProperty("matchingServiceAdapterEntityId") final String matchingServiceAdapterEntityId,
            @JsonProperty("encryptedMatchingDatasetAssertion") final String encryptedMatchingDatasetAssertion,
            @JsonProperty("authnStatementAssertion") final String authnStatementAssertion,
            @JsonProperty("persistentId") final PersistentId persistentId) {

        super(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTime,
                assertionConsumerServiceIndex,
                sessionId,
                transactionSupportsEidas,
                registering,
                identityProviderEntityId,
                relayState,
                idpLevelOfAssurance,
                matchingServiceAdapterEntityId,
                encryptedMatchingDatasetAssertion,
                authnStatementAssertion,
                persistentId
        );
    }
}
