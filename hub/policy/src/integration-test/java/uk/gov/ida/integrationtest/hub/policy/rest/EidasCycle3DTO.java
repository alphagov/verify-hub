package uk.gov.ida.integrationtest.hub.policy.rest;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

// TODO haven't considered class location - test only!
@Immutable
public final class EidasCycle3DTO {

    private SessionId sessionId;
    private String requestId = "requestId";
    private String identityProviderEntityId = "identityProviderEntityId";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private String requestIssuerEntityId = "requestIssuerEntityId";
    private String matchingServiceAssertion = "matchingServiceAssertion";
    private String relayState = "relayState";
    private URI assertionConsumerServiceUri = URI.create("http://assertionconsumeruri");
    private String matchingServiceAdapterEntityId = "matchingServiceAdapterEntityId";
    private PersistentId persistentId = new PersistentId("nameId");
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private boolean transactionSupportsEidas = true;

    @Nullable
    private String encryptedIdentityAssertion = "encryptedIdentityAssertion";

    @SuppressWarnings("unused") //Needed for JAXB
    private EidasCycle3DTO() {}

    public EidasCycle3DTO(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIdentityProviderEntityId() {
        return identityProviderEntityId;
    }

    public DateTime getSessionExpiryTimestamp() {
        return sessionExpiryTimestamp;
    }

    public String getRequestIssuerEntityId() {
        return requestIssuerEntityId;
    }

    public String getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public String getMatchingServiceAdapterEntityId() {
        return matchingServiceAdapterEntityId;
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public String getEncryptedIdentityAssertion() {
        return encryptedIdentityAssertion;
    }

    public boolean getTransactionSupportsEidas() {
        return transactionSupportsEidas;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EidasCycle3DTO{");
        sb.append("sessionId=").append(sessionId);
        sb.append(", requestId='").append(requestId).append('\'');
        sb.append(", identityProviderEntityId='").append(identityProviderEntityId).append('\'');
        sb.append(", sessionExpiryTimestamp=").append(sessionExpiryTimestamp);
        sb.append(", requestIssuerEntityId='").append(requestIssuerEntityId).append('\'');
        sb.append(", matchingServiceAssertion='").append(matchingServiceAssertion).append('\'');
        sb.append(", relayState=").append(getRelayState());
        sb.append(", assertionConsumerServiceUri=").append(assertionConsumerServiceUri);
        sb.append(", matchingServiceAdapterEntityId='").append(matchingServiceAdapterEntityId).append('\'');
        sb.append(", persistentId=").append(persistentId);
        sb.append(", levelOfAssurance=").append(levelOfAssurance);
        sb.append(", transactionSupportsEidas=").append(transactionSupportsEidas);
        sb.append(", encryptedIdentityAssertion='").append(encryptedIdentityAssertion).append('\'');
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

        EidasCycle3DTO that = (EidasCycle3DTO) o;

        return transactionSupportsEidas == that.transactionSupportsEidas &&
            Objects.equals(sessionId, that.sessionId) &&
            Objects.equals(requestId, that.requestId) &&
            Objects.equals(identityProviderEntityId, that.identityProviderEntityId) &&
            Objects.equals(sessionExpiryTimestamp, that.sessionExpiryTimestamp) &&
            Objects.equals(requestIssuerEntityId, that.requestIssuerEntityId) &&
            Objects.equals(matchingServiceAssertion, that.matchingServiceAssertion) &&
            Objects.equals(relayState, that.relayState) &&
            Objects.equals(assertionConsumerServiceUri, that.assertionConsumerServiceUri) &&
            Objects.equals(matchingServiceAdapterEntityId, that.matchingServiceAdapterEntityId) &&
            Objects.equals(persistentId, that.persistentId) &&
            levelOfAssurance == that.levelOfAssurance &&
            Objects.equals(encryptedIdentityAssertion, that.encryptedIdentityAssertion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            sessionId,
            requestId,
            identityProviderEntityId,
            sessionExpiryTimestamp,
            requestIssuerEntityId,
            matchingServiceAssertion,
            relayState,
            assertionConsumerServiceUri,
            matchingServiceAdapterEntityId,
            persistentId,
            levelOfAssurance,
            transactionSupportsEidas,
            encryptedIdentityAssertion);
    }
}
