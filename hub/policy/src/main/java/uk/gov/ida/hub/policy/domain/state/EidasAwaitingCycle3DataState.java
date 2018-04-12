package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.StandardToStringStyle;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

public final class EidasAwaitingCycle3DataState extends AbstractAwaitingCycle3DataState implements ResponseProcessingState, Serializable {

    private static final long serialVersionUID = -9056285913241958733L;

    private final String encryptedIdentityAssertion;

    public EidasAwaitingCycle3DataState(
        final String requestId,
        final String requestIssuerId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final String identityProviderEntityId,
        final String matchingServiceAdapterEntityId,
        final Optional<String> relayState,
        final PersistentId persistentId,
        final LevelOfAssurance levelOfAssurance,
        final String encryptedIdentityAssertion) {

        super(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            identityProviderEntityId,
            matchingServiceAdapterEntityId,
            relayState,
            persistentId,
            levelOfAssurance);

        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
    }

    public String getEncryptedIdentityAssertion() {
        return encryptedIdentityAssertion;
    }

    @Override
    public String toString() {
        final StandardToStringStyle style = new StandardToStringStyle();
        style.setUseIdentityHashCode(false);
        return ReflectionToStringBuilder.toString(this, style);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EidasAwaitingCycle3DataState that = (EidasAwaitingCycle3DataState) o;

        return Objects.equals(encryptedIdentityAssertion, that.encryptedIdentityAssertion) &&
            Objects.equals(getIdentityProviderEntityId(), that.getIdentityProviderEntityId()) &&
            Objects.equals(getMatchingServiceEntityId(), that.getMatchingServiceEntityId()) &&
            Objects.equals(getRelayState(), that.getRelayState()) &&
            Objects.equals(getPersistentId(), that.getPersistentId()) &&
            getLevelOfAssurance() == that.getLevelOfAssurance() &&
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
            encryptedIdentityAssertion,
            getIdentityProviderEntityId(),
            getMatchingServiceEntityId(),
            getRelayState(),
            getPersistentId(),
            getLevelOfAssurance(),
            getTransactionSupportsEidas(),
            getRequestId(),
            getRequestIssuerEntityId(),
            getSessionExpiryTimestamp(),
            getAssertionConsumerServiceUri(),
            getSessionId());
    }
}
