package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Objects;

public final class SuccessfulMatchStateTransitional extends AbstractSuccessfulMatchState {

    private final boolean isRegistering;

    public SuccessfulMatchStateTransitional(
            String requestId,
            DateTime sessionExpiryTimestamp,
            String identityProviderEntityId,
            String matchingServiceAssertion,
            Optional<String> relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri,
            SessionId sessionId,
            LevelOfAssurance levelOfAssurance,
            boolean isRegistering,
            boolean transactionSupportsEidas) {

        super(
                requestId,
                sessionExpiryTimestamp,
                identityProviderEntityId,
                matchingServiceAssertion,
                relayState,
                requestIssuerId,
                assertionConsumerServiceUri,
                sessionId,
                levelOfAssurance,
                transactionSupportsEidas);

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

        SuccessfulMatchStateTransitional that = (SuccessfulMatchStateTransitional) o;

        return Objects.equals(isRegistering, that.isRegistering) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isRegistering, super.hashCode());
    }
}
