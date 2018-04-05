package uk.gov.ida.hub.policy.domain.state;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public final class EidasCycle0And1MatchRequestSentState extends EidasMatchRequestSentState {

    private static final long serialVersionUID = -5585201555901105188L;

    public EidasCycle0And1MatchRequestSentState(
            final String requestId,
            final String requestIssuerEntityId,
            final DateTime sessionExpiryTimestamp,
            final URI assertionConsumerServiceUri,
            final SessionId sessionId,
            final boolean transactionSupportsEidas,
            final String identityProviderEntityId,
            final String relayState,
            final LevelOfAssurance idpLevelOfAssurance,
            final String matchingServiceAdapterEntityId,
            final String encryptedIdentityAssertion,
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
                matchingServiceAdapterEntityId,
                encryptedIdentityAssertion,
                persistentId
        );
    }
}
