package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Optional;

public abstract class AbstractMatchRequestSentState extends AbstractState implements ResponseProcessingState, WaitingForMatchingServiceResponseState {

    private static final long serialVersionUID = -3235068474581736050L;

    @JsonProperty
    private final String identityProviderEntityId;
    @JsonProperty
    private final String relayState;
    @JsonProperty
    private final LevelOfAssurance idpLevelOfAssurance;
    @JsonProperty
    private final String matchingServiceAdapterEntityId;
    @JsonProperty
    private final DateTime requestSentTime;

    protected AbstractMatchRequestSentState(
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
            final Boolean forceAuthentication) {

        super(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                forceAuthentication
        );

        this.identityProviderEntityId = identityProviderEntityId;
        this.relayState = relayState;
        this.idpLevelOfAssurance = idpLevelOfAssurance;
        this.matchingServiceAdapterEntityId = matchingServiceAdapterEntityId;
        this.requestSentTime = DateTime.now(DateTimeZone.UTC);
    }

    @Override
    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
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
