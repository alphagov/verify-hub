package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public class EidasSuccessfulMatchState extends AbstractSuccessfulMatchState {

    private static final long serialVersionUID = 7677160699140073010L;

    public EidasSuccessfulMatchState(
            String requestId,
            DateTime sessionExpiryTimestamp,
            String identityProviderEntityId,
            String matchingServiceAssertion,
            String relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri,
            SessionId sessionId,
            LevelOfAssurance levelOfAssurance,
            boolean transactionSupportsEidas) {

        super(
                requestId,
                sessionExpiryTimestamp,
                identityProviderEntityId,
                matchingServiceAssertion,
                Optional.fromNullable(relayState),
                requestIssuerId,
                assertionConsumerServiceUri,
                sessionId,
                levelOfAssurance,
                transactionSupportsEidas);
    }
}
