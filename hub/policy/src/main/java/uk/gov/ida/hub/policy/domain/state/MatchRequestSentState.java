package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

public abstract class MatchRequestSentState extends AbstractState implements ResponseProcessingState, WaitingForMatchingServiceResponseState {

    private final String identityProviderEntityId;
    private final Optional<String> relayState;
    private final LevelOfAssurance idpLevelOfAssurance;
    private final String matchingServiceAdapterEntityId;
    private final DateTime requestSentTime;

    protected MatchRequestSentState(
        final String requestId,
        final String requestIssuerEntityId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final String identityProviderEntityId,
        final Optional<String> relayState,
        final LevelOfAssurance idpLevelOfAssurance,
        final String matchingServiceAdapterEntityId) {

        super(
            requestId,
            requestIssuerEntityId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas
        );

        this.identityProviderEntityId = identityProviderEntityId;
        this.relayState = relayState;
        this.idpLevelOfAssurance = idpLevelOfAssurance;
        this.matchingServiceAdapterEntityId = matchingServiceAdapterEntityId;
        this.requestSentTime = DateTime.now();
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public DateTime getRequestSentTime() {
        return requestSentTime;
    }

    public LevelOfAssurance getIdpLevelOfAssurance() {
        return idpLevelOfAssurance;
    }

    public String getMatchingServiceAdapterEntityId() {
        return matchingServiceAdapterEntityId;
    }
}
