package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TestSessionDto {


    private SessionId sessionId;
    private String requestId;
    private DateTime sessionExpiryTimestamp;
    private String identityProviderEntityId;
    private String matchingServiceAssertion;
    private String relayState;
    private String requestIssuerId;
    private URI assertionConsumerServiceUri;
    private List<LevelOfAssurance> levelsOfAssurance;
    private Boolean useExactComparisonType;
    private Boolean forceAuthentication;
    private boolean registering;
    private List<String> availableIdentityProviders;
    private boolean transactionSupportsEidas;
    private LevelOfAssurance requestedLoa;
    private Set<String> encryptedAssertions;
    private CountrySignedResponseContainer countrySignedResponseContainer;

    @SuppressWarnings("unused") //Needed for JAXB
    private TestSessionDto() {
    }

    public TestSessionDto(SessionId sessionId,
                          String requestId,
                          DateTime sessionExpiryTimestamp,
                          String identityProviderEntityId,
                          String relayState,
                          String requestIssuerId,
                          String matchingServiceAssertion,
                          URI assertionConsumerServiceUri,
                          List<LevelOfAssurance> levelsOfAssurance,
                          Boolean useExactComparisonType,
                          boolean transactionSupportsEidas) {
        this(
                sessionId,
                requestId,
                sessionExpiryTimestamp,
                identityProviderEntityId,
                matchingServiceAssertion,
                relayState,
                requestIssuerId,
                assertionConsumerServiceUri,
                levelsOfAssurance,
                useExactComparisonType,
                false,
                LevelOfAssurance.LEVEL_2,
                false,
                new ArrayList<>(),
                transactionSupportsEidas,
                null,
                null);
    }

    public TestSessionDto(
            SessionId sessionId,
            String requestId,
            DateTime sessionExpiryTimestamp,
            String identityProviderEntityId,
            String matchingServiceAssertion,
            String relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri,
            List<LevelOfAssurance> levelsOfAssurance,
            Boolean useExactComparisonType,
            Boolean registering,
            LevelOfAssurance requestedLoa,
            Boolean forceAuthentication,
            List<String> availableIdentityProviders,
            boolean transactionSupportsEidas,
            Set<String> encryptedAssertions,
            CountrySignedResponseContainer countrySignedResponseContainer) {

        this.sessionId = sessionId;
        this.requestId = requestId;
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        this.identityProviderEntityId = identityProviderEntityId;
        this.matchingServiceAssertion = matchingServiceAssertion;
        this.relayState = relayState;
        this.requestIssuerId = requestIssuerId;
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        this.levelsOfAssurance = levelsOfAssurance;
        this.useExactComparisonType = useExactComparisonType;
        this.registering = registering;
        this.requestedLoa = requestedLoa;
        this.forceAuthentication = forceAuthentication;
        this.availableIdentityProviders = availableIdentityProviders;
        this.transactionSupportsEidas = transactionSupportsEidas;
        this.encryptedAssertions = encryptedAssertions;
        this.countrySignedResponseContainer = countrySignedResponseContainer;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public DateTime getSessionExpiryTimestamp() {
        return sessionExpiryTimestamp;
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

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public Optional<Boolean> getForceAuthentication() {
        return Optional.ofNullable(forceAuthentication);
    }

    public boolean isRegistering() {
        return registering;
    }

    public LevelOfAssurance getRequestedLoa() {
        return requestedLoa;
    }

    public List<String> getAvailableIdentityProviders() {
        return availableIdentityProviders;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }

    public Boolean getUseExactComparisonType() {
        return useExactComparisonType;
    }

    public boolean getTransactionSupportsEidas() {
        return transactionSupportsEidas;
    }

    public Set<String> getEncryptedAssertions() { return encryptedAssertions; }

    public CountrySignedResponseContainer getCountrySignedResponseContainer() { return countrySignedResponseContainer; }
}
