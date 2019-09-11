package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.StandardToStringStyle;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractSuccessfulMatchState extends AbstractState implements ResponseProcessingState, ResponsePreparedState, Serializable {

    private static final long serialVersionUID = 7151768021473278078L;

    @JsonProperty
    protected final String identityProviderEntityId;
    @JsonProperty
    protected final String matchingServiceAssertion;
    @JsonProperty
    protected final String relayState;
    @JsonProperty
    protected final LevelOfAssurance levelOfAssurance;

    public AbstractSuccessfulMatchState(
            String requestId,
            DateTime sessionExpiryTimestamp,
            String identityProviderEntityId,
            String matchingServiceAssertion,
            String relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri,
            SessionId sessionId,
            LevelOfAssurance levelOfAssurance,
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
        this.matchingServiceAssertion = matchingServiceAssertion;
        this.relayState = relayState;
        this.levelOfAssurance = levelOfAssurance;
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
    }

    public String getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
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

        AbstractSuccessfulMatchState that = (AbstractSuccessfulMatchState) o;

        return Objects.equals(identityProviderEntityId, that.getIdentityProviderEntityId()) &&
                Objects.equals(matchingServiceAssertion, that.getMatchingServiceAssertion()) &&
                Objects.equals(relayState, that.relayState) &&
                levelOfAssurance == that.getLevelOfAssurance() &&
                getTransactionSupportsEidas() == that.getTransactionSupportsEidas() &&
                Objects.equals(getRequestId(), that.getRequestId()) &&
                Objects.equals(getRequestIssuerEntityId(), that.getRequestIssuerEntityId()) &&
                Objects.equals(getSessionExpiryTimestamp(), that.getSessionExpiryTimestamp()) &&
                Objects.equals(getAssertionConsumerServiceUri(), that.getAssertionConsumerServiceUri()) &&
                Objects.equals(getForceAuthentication(), that.getForceAuthentication()) &&
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
                getForceAuthentication(),
                getSessionId());
    }
}
