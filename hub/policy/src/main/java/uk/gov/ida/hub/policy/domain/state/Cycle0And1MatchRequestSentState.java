package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class Cycle0And1MatchRequestSentState extends MatchRequestSentState {

    private static final long serialVersionUID = -1318475127132434126L;

    @JsonProperty
    private final String encryptedMatchingDatasetAssertion;
    @JsonProperty
    private final String authnStatementAssertion;
    @JsonProperty
    private final PersistentId persistentId;

    @JsonCreator
    public Cycle0And1MatchRequestSentState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerEntityId") final String requestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
            @JsonProperty("registering") final boolean registering,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("idpLevelOfAssurance") final LevelOfAssurance idpLevelOfAssurance,
            @JsonProperty("matchingServiceAdapterEntityId") final String matchingServiceAdapterEntityId,
            @JsonProperty("encryptedMatchingDatasetAssertion") final String encryptedMatchingDatasetAssertion,
            @JsonProperty("authnStatementAssertion") final String authnStatementAssertion,
            @JsonProperty("persistentId") final PersistentId persistentId) {

        super(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                identityProviderEntityId,
                relayState,
                idpLevelOfAssurance,
                registering,
                matchingServiceAdapterEntityId
        );

        this.encryptedMatchingDatasetAssertion = encryptedMatchingDatasetAssertion;
        this.authnStatementAssertion = authnStatementAssertion;
        this.persistentId = persistentId;
    }

    public String getAuthnStatementAssertion() {
        return authnStatementAssertion;
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public String getEncryptedMatchingDatasetAssertion() {
        return encryptedMatchingDatasetAssertion;
    }
}
