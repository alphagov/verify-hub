package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class Cycle0And1MatchRequestSentState extends MatchRequestSentState {

    private final String encryptedMatchingDatasetAssertion;
    private final String authnStatementAssertion;
    private final PersistentId persistentId;

    public Cycle0And1MatchRequestSentState(
        final String requestId,
        final String requestIssuerEntityId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final String identityProviderEntityId,
        final Optional<String> relayState,
        final LevelOfAssurance idpLevelOfAssurance,
        final String matchingServiceAdapterEntityId,
        final String encryptedMatchingDatasetAssertion,
        final String authnStatementAssertion,
        final PersistentId persistentId) {

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
