package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;

public abstract class AbstractAwaitingCycle3DataState extends AbstractState implements ResponseProcessingState, Serializable {

    private static final long serialVersionUID = -3139156310818993792L;

    @JsonProperty
    private final String identityProviderEntityId;
    @JsonProperty
    private final String matchingServiceEntityId;
    @JsonProperty
    private final String relayState;
    @JsonProperty
    private final PersistentId persistentId;
    @JsonProperty
    private final LevelOfAssurance levelOfAssurance;

    public AbstractAwaitingCycle3DataState(
        final String requestId,
        final String requestIssuerId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final String identityProviderEntityId,
        final String matchingServiceEntityId,
        final String relayState,
        final PersistentId persistentId,
        final LevelOfAssurance levelOfAssurance,
        final Boolean forceAuthentication) {

        super(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                forceAuthentication
        );

        this.identityProviderEntityId = identityProviderEntityId;
        this.matchingServiceEntityId = matchingServiceEntityId;
        this.relayState = relayState;
        this.persistentId = persistentId;
        this.levelOfAssurance = levelOfAssurance;
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
    }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }

    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }
}
