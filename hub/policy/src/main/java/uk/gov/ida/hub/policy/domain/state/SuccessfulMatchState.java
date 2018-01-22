package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Objects;

public final class SuccessfulMatchState extends AbstractSuccessfulMatchState {
    public SuccessfulMatchState(String requestId, DateTime sessionExpiryTimestamp, String identityProviderEntityId, String matchingServiceAssertion, Optional<String> relayState, String requestIssuerId, URI assertionConsumerServiceUri, SessionId sessionId, LevelOfAssurance levelOfAssurance, boolean transactionSupportsEidas) {
        super(requestId, sessionExpiryTimestamp, identityProviderEntityId, matchingServiceAssertion, relayState, requestIssuerId, assertionConsumerServiceUri, sessionId, levelOfAssurance, transactionSupportsEidas);
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

        return Objects.equals(identityProviderEntityId, that.identityProviderEntityId) &&
            Objects.equals(matchingServiceAssertion, that.matchingServiceAssertion) &&
            Objects.equals(relayState, that.relayState) &&
            levelOfAssurance == that.levelOfAssurance &&
            getTransactionSupportsEidas() == that.getTransactionSupportsEidas() &&
            Objects.equals(getRequestId(), that.getRequestId()) &&
            Objects.equals(getRequestIssuerEntityId(), that.getRequestIssuerEntityId()) &&
            Objects.equals(getSessionExpiryTimestamp(), that.getSessionExpiryTimestamp()) &&
            Objects.equals(getAssertionConsumerServiceUri(), that.getAssertionConsumerServiceUri()) &&
            Objects.equals(getSessionId(), that.getSessionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            identityProviderEntityId,
            matchingServiceAssertion,
            relayState,
            levelOfAssurance,
            getTransactionSupportsEidas(),
            getRequestId(),
            getRequestIssuerEntityId(),
            getSessionExpiryTimestamp(),
            getAssertionConsumerServiceUri(),
            getSessionId());
    }
}
