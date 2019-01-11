package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class UserAccountCreatedState extends AbstractState implements ResponseProcessingState, ResponsePreparedState, Serializable {

    private static final long serialVersionUID = -1020619173417432390L;

    @JsonProperty
    private final String identityProviderEntityId;
    @JsonProperty
    private final String matchingServiceAssertion;
    @JsonProperty
    private final String relayState;
    @JsonProperty
    private final LevelOfAssurance levelOfAssurance;
    @JsonProperty
    private final boolean registering;

    @JsonCreator
    public UserAccountCreatedState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("matchingServiceAssertion") final String matchingServiceAssertion,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("levelOfAssurance") final LevelOfAssurance levelOfAssurance,
            @JsonProperty("registering") final boolean registering,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas) {

        super(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            null
        );

        this.identityProviderEntityId = identityProviderEntityId;
        this.matchingServiceAssertion = matchingServiceAssertion;
        this.relayState = relayState;
        this.levelOfAssurance = levelOfAssurance;
        this.registering = registering;
    }

    @Override
    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
    }

    public String getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public boolean isRegistering() {
        return registering;
    }
}
