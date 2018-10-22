package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Objects;

public final class SuccessfulMatchState extends AbstractSuccessfulMatchState {

    private static final long serialVersionUID = 383573706638201670L;

    private final boolean isRegistering;

    public SuccessfulMatchState(
            String requestId,
            DateTime sessionExpiryTimestamp,
            String identityProviderEntityId,
            String matchingServiceAssertion,
            String relayState,
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
                Optional.fromNullable(relayState),
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

        SuccessfulMatchState that = (SuccessfulMatchState) o;

        return Objects.equals(isRegistering, that.isRegistering)
                && Objects.equals(getForceAuthentication(), that.getForceAuthentication())
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isRegistering, super.hashCode());
    }
}
