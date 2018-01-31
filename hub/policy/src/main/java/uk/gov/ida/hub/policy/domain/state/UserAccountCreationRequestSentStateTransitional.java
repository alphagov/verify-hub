package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class UserAccountCreationRequestSentStateTransitional extends MatchRequestSentStateTransitional implements Serializable {

    public UserAccountCreationRequestSentStateTransitional(
        final String requestId,
        final String requestIssuerEntityId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final String identityProviderEntityId,
        final Optional<String> relayState,
        final LevelOfAssurance idpLevelOfAssurance,
        final boolean registering,
        final String matchingServiceAdapterEntityId) {

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
    }
}
