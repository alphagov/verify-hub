package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class AwaitingCycle3DataState extends AbstractAwaitingCycle3DataState implements ResponseProcessingState, Serializable {
    private final String encryptedMatchingDatasetAssertion;
    private final String authnStatementAssertion;
    private final boolean registering;

    public AwaitingCycle3DataState(
            String requestId,
            String identityProviderEntityId,
            DateTime sessionExpiryTimestamp,
            String requestIssuerId,
            String encryptedMatchingDatasetAssertion,
            String authnStatementAssertion,
            Optional<String> relayState,
            URI assertionConsumerServiceUri,
            String matchingServiceEntityId,
            SessionId sessionId,
            PersistentId persistentId,
            LevelOfAssurance levelOfAssurance,
            boolean registering,
            boolean transactionSupportsEidas) {

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
            levelOfAssurance);

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
