package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class Cycle3MatchRequestSentState extends Cycle0And1MatchRequestSentState implements Serializable {

    public Cycle3MatchRequestSentState(
        final String requestId,
        final String requestIssuerEntityId,
        final DateTime sessionExpiryTime,
        final URI assertionConsumerServiceIndex,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final String identityProviderEntityId,
        final Optional<String> relayState,
        final LevelOfAssurance idpLevelOfAssurance,
        final boolean registering,
        final String matchingServiceAdapterEntityId,
        final String encryptedMatchingDatasetAssertion,
        final String authnStatementAssertion,
        final PersistentId persistentId) {

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
