package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.net.URI;
import java.util.List;

import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;

public class EidasCycle0And1MatchRequestSentStateBuilder {

    private String encryptedIdentityAssertion = "encryptedIdentityAssertion";
    private PersistentId persistentId = aPersistentId().build();
    private Boolean forceAuthentication = false;
    private CountrySignedResponseContainer countrySignedResponseContainer;

    public static EidasCycle0And1MatchRequestSentStateBuilder anEidasCycle0And1MatchRequestSentState() {
        return new EidasCycle0And1MatchRequestSentStateBuilder();
    }

    public EidasCycle0And1MatchRequestSentState build() {
        return new EidasCycle0And1MatchRequestSentState(
            "requestId",
            "requestIssuerId",
            DateTime.now(DateTimeZone.UTC).plusMinutes(10),
            URI.create("assertionConsumerServiceUri"),
            new SessionId("sessionId"),
            true,
            "identityProviderEntityId",
            null,
            LevelOfAssurance.LEVEL_2,
            "matchingServiceAdapterEntityId",
            encryptedIdentityAssertion,
            persistentId,
            forceAuthentication,
            countrySignedResponseContainer
        );
    }

    public EidasCycle0And1MatchRequestSentStateBuilder withEncryptedIdentityAssertion(final String encryptedIdentityAssertion) {
        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
        return this;
    }

    public EidasCycle0And1MatchRequestSentStateBuilder withPersistentId(final PersistentId persistentId) {
        this.persistentId = persistentId;
        return this;
    }

    public EidasCycle0And1MatchRequestSentStateBuilder withForceAuthentication(final Boolean forceAuthentication) {
        this.forceAuthentication = forceAuthentication;
        return this;
    }
    public EidasCycle0And1MatchRequestSentStateBuilder withCountrySignedResponseContainer(CountrySignedResponseContainer countrySignedResponseContainer) {
        this.countrySignedResponseContainer = countrySignedResponseContainer;
        return this;
    }
    public EidasCycle0And1MatchRequestSentStateBuilder withCountrySignedResponseContainer() {
        this.countrySignedResponseContainer = new CountrySignedResponseContainer(
                "MIEBASE64STRING==", List.of("MIEKEY=="), "http://country.com"
        );
        return this;
    }
}
