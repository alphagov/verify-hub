package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class UserAccountCreatedState extends AbstractState implements ResponseProcessingState, ResponsePreparedState, Serializable {

    private static final long serialVersionUID = -1020619173417432390L;

    private final String identityProviderEntityId;
    private final String matchingServiceAssertion;
    private final String relayState;
    private final LevelOfAssurance levelOfAssurance;
    private final boolean registering;

    public UserAccountCreatedState(
            final String requestId,
            final String requestIssuerId,
            final DateTime sessionExpiryTimestamp,
            final URI assertionConsumerServiceUri,
            final SessionId sessionId,
            final String identityProviderEntityId,
            final String matchingServiceAssertion,
            final String relayState,
            LevelOfAssurance levelOfAssurance,
            boolean registering,
            boolean transactionSupportsEidas) {
        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas, null);
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
