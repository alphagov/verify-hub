package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class NonMatchingRequestReceivedState extends AbstractState implements ResponsePreparedState {

    private final String identityProviderEntityId;
    private final List<String> assertions;
    private final String relayState;
    private final LevelOfAssurance levelOfAssurance;
    private final boolean isRegistering;

    public NonMatchingRequestReceivedState(
            String requestId,
            DateTime sessionExpiryTimestamp,
            String identityProviderEntityId,
            List<String> assertions,
            String relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri,
            SessionId sessionId,
            LevelOfAssurance levelOfAssurance,
            boolean isRegistering,
            boolean transactionSupportsEidas) {
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
        this.assertions = assertions;
        this.relayState = relayState;
        this.levelOfAssurance = levelOfAssurance;

        this.isRegistering = isRegistering;
    }

    public boolean isRegistering() {
        return isRegistering;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NonMatchingRequestReceivedState that = (NonMatchingRequestReceivedState) o;

        return Objects.equals(isRegistering, that.isRegistering) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isRegistering, super.hashCode());
    }

    @Override
    public Optional<String> getRelayState() {
        return Optional.of(relayState);
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
    }

    public List<String> getAssertions() {
        return assertions;
    }
}
