package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.net.URI;
import java.util.Optional;

public class EidasCycle0And1MatchRequestSentState extends EidasMatchRequestSentState {

    private static final long serialVersionUID = -5585201555901105188L;

    @JsonProperty
    private final String encryptedIdentityAssertion;
    @JsonProperty
    private final PersistentId persistentId;
    @JsonProperty
    private final CountrySignedResponseContainer countrySignedResponseContainer;

    @JsonCreator
    public EidasCycle0And1MatchRequestSentState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerEntityId") final String requestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("idpLevelOfAssurance") final LevelOfAssurance idpLevelOfAssurance,
            @JsonProperty("matchingServiceAdapterEntityId") final String matchingServiceAdapterEntityId,
            @JsonProperty("encryptedIdentityAssertion") final String encryptedIdentityAssertion,
            @JsonProperty("persistentId") final PersistentId persistentId,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication,
            @JsonProperty("countrySignedResponseContainer") final CountrySignedResponseContainer countrySignedResponseContainer) {

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
                forceAuthentication);

        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
        this.persistentId = persistentId;
        this.countrySignedResponseContainer = countrySignedResponseContainer;
    }

    public String getEncryptedIdentityAssertion() {
        return encryptedIdentityAssertion;
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public Optional<CountrySignedResponseContainer> getCountrySignedResponseContainer() {
        return Optional.ofNullable(countrySignedResponseContainer);
    }
}
