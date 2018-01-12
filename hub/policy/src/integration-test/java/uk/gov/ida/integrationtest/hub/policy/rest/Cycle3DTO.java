package uk.gov.ida.integrationtest.hub.policy.rest;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import javax.annotation.Nullable;
import java.net.URI;

// TODO haven't considered class location - test only!
public class Cycle3DTO {

    private SessionId sessionId;
    private String requestId = "request-id";
    private String identityProviderEntityId = "idp-id";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private String requestIssuerId = "request-issuer-id";
    private String matchingServiceAssertion = "matching-service-assertion";
    private String authnStatementAssertion = "authn-statement-assertion";
    private Optional<String> relayState = Optional.fromNullable("relay-state");
    private URI assertionConsumerServiceUri = URI.create("http://assertionconsumeruri");
    private String matchingServiceEntityId = "matching-service-entity-id";
    private PersistentId persistentId = new PersistentId("persistent-id");
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private boolean transactionSupportsEidas = false;
    private boolean registering = false;

    @Nullable
    private String encryptedMatchingDatasetAssertion = matchingServiceAssertion;

    @SuppressWarnings("unused") //Needed for JAXB
    private Cycle3DTO() {}

    public Cycle3DTO(SessionId sessionId) {
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

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

    public String getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public String getAuthnStatementAssertion() {
        return authnStatementAssertion;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public String getEncryptedMatchingDatasetAssertion() {
        return encryptedMatchingDatasetAssertion;
    }

    public boolean getTransactionSupportsEidas() {
        return transactionSupportsEidas;
    }

    public boolean isRegistering() {
        return registering;
    }
}
