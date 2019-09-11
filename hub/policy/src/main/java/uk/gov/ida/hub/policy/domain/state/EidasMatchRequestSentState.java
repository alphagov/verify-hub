package uk.gov.ida.hub.policy.domain.state;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Objects;

public abstract class EidasMatchRequestSentState extends AbstractMatchRequestSentState {

    private static final long serialVersionUID = -186027641698264989L;

    protected EidasMatchRequestSentState(
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
            identityProviderEntityId,
            relayState,
            idpLevelOfAssurance,
            matchingServiceAdapterEntityId,
            forceAuthentication
        );
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + "{");
        sb.append(", identityProviderEntityId='").append(getIdentityProviderEntityId()).append('\'');
        sb.append(", relayState=").append(getRelayState());
        sb.append(", requestSentTime=").append(getRequestSentTime());
        sb.append(", idpLevelOfAssurance=").append(getIdpLevelOfAssurance());
        sb.append(", matchingServiceEntityId='").append(getMatchingServiceAdapterEntityId()).append('\'');
        sb.append(", requestId='").append(getRequestId()).append('\'');
        sb.append(", sessionId=").append(getSessionId());
        sb.append(", requestIssuerEntityId='").append(getRequestIssuerEntityId()).append('\'');
        sb.append(", sessionExpiryTimestamp=").append(getSessionExpiryTimestamp());
        sb.append(", assertionConsumerServiceUri=").append(getAssertionConsumerServiceUri());
        sb.append(", transactionSupportsEidas=").append(getTransactionSupportsEidas());
        sb.append(", forceAuthentication=").append(getForceAuthentication().orElse(null));
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EidasMatchRequestSentState that = (EidasMatchRequestSentState) o;

        return getTransactionSupportsEidas() == that.getTransactionSupportsEidas() &&
            Objects.equals(getRequestId(), that.getRequestId()) &&
            Objects.equals(getRequestIssuerEntityId(), that.getRequestIssuerEntityId()) &&
            Objects.equals(getSessionExpiryTimestamp(), that.getSessionExpiryTimestamp()) &&
            Objects.equals(getAssertionConsumerServiceUri(), that.getAssertionConsumerServiceUri()) &&
            Objects.equals(getSessionId(), that.getSessionId()) &&
            Objects.equals(getIdentityProviderEntityId(), that.getIdentityProviderEntityId()) &&
            Objects.equals(getRelayState(), that.getRelayState()) &&
            Objects.equals(getRequestSentTime(), that.getRequestSentTime()) &&
            getIdpLevelOfAssurance() == that.getIdpLevelOfAssurance() &&
            Objects.equals(getMatchingServiceAdapterEntityId(), that.getMatchingServiceAdapterEntityId()) &&
            Objects.equals(getForceAuthentication(), that.getForceAuthentication());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getTransactionSupportsEidas(),
            getRequestId(),
            getRequestIssuerEntityId(),
            getSessionExpiryTimestamp(),
            getAssertionConsumerServiceUri(),
            getSessionId(),
            getIdentityProviderEntityId(),
            getRelayState(),
            getRequestSentTime(),
            getIdpLevelOfAssurance(),
            getMatchingServiceAdapterEntityId(),
            getForceAuthentication());
    }
}
