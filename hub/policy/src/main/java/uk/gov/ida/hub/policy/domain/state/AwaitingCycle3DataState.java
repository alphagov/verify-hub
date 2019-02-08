package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class AwaitingCycle3DataState extends AbstractAwaitingCycle3DataState {

    private static final long serialVersionUID = 2909622650570769370L;

    @JsonProperty
    private final String encryptedMatchingDatasetAssertion;
    @JsonProperty
    private final String authnStatementAssertion;
    @JsonProperty
    private final boolean registering;

    @JsonCreator
    public AwaitingCycle3DataState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("encryptedMatchingDatasetAssertion") final String encryptedMatchingDatasetAssertion,
            @JsonProperty("authnStatementAssertion") final String authnStatementAssertion,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("matchingServiceEntityId") final String matchingServiceEntityId,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("persistentId") final PersistentId persistentId,
            @JsonProperty("levelOfAssurance") final LevelOfAssurance levelOfAssurance,
            @JsonProperty("registering") final boolean registering,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

        super(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                identityProviderEntityId,
                matchingServiceEntityId,
                relayState,
                persistentId,
                levelOfAssurance,
                null
        );

        this.encryptedMatchingDatasetAssertion = encryptedMatchingDatasetAssertion;
        this.authnStatementAssertion = authnStatementAssertion;
        this.registering = registering;
    }
    public String getAuthnStatementAssertion() {
        return authnStatementAssertion;
    }

    public String getEncryptedMatchingDatasetAssertion() {
        return encryptedMatchingDatasetAssertion;
    }

    public boolean isRegistering() {
        return registering;
    }
}
