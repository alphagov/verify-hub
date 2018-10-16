package uk.gov.ida.hub.policy.domain.state;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class EidasUserAccountCreationRequestSentState extends EidasMatchRequestSentState implements Serializable {

    private static final long serialVersionUID = -3124130170426481847L;

    public EidasUserAccountCreationRequestSentState(
            final String requestId,
            final String requestIssuerEntityId,
            final DateTime sessionExpiryTimestamp,
            final URI assertionConsumerServiceUri,
            final SessionId sessionId,
            final String identityProviderEntityId,
            final String relayState,
            final LevelOfAssurance idpLevelOfAssurance,
            final String matchingServiceAdapterEntityId,
            final Boolean forceAuthentication) {

        super(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                true,
                identityProviderEntityId,
                relayState,
                idpLevelOfAssurance,
                matchingServiceAdapterEntityId,
                forceAuthentication
        );
    }
}
