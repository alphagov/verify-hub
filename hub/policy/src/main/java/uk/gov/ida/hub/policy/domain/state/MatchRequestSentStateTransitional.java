package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public abstract class MatchRequestSentStateTransitional extends AbstractMatchRequestSentState {

    private final boolean registering;
    private final DateTime requestSentTime;

    protected MatchRequestSentStateTransitional(
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
            final String matchingServiceAdapterEntityId,
            final DateTime requestSentTime) {

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

        this.registering = registering;
        this.requestSentTime = requestSentTime;
    }

    public boolean isRegistering() {
        return registering;
    }

    @Override
    public DateTime getRequestSentTime() {
        return requestSentTime;
    }
}
