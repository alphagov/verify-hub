package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.net.URI;
import java.util.List;

public class EidasAwaitingCycle3DataStateBuilder {

    private SessionId sessionId = new SessionId("sessionId");
    private CountrySignedResponseContainer countrySignedResponseContainer;

    public static EidasAwaitingCycle3DataStateBuilder anEidasAwaitingCycle3DataState() {
        return new EidasAwaitingCycle3DataStateBuilder();
    }

    public EidasAwaitingCycle3DataStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public EidasAwaitingCycle3DataState build() {
        return new EidasAwaitingCycle3DataState(
            "requestId",
            "requestIssuerId",
            DateTime.now(DateTimeZone.UTC).plusMinutes(10),
            URI.create("assertionConsumerServiceUri"),
            sessionId,
            true,
            "identityProviderEntityId",
            "matchingServiceAdapterEntityId",
            "relayState",
            new PersistentId("nameId"),
            LevelOfAssurance.LEVEL_2,
            "encryptedIdentityAssertion",
            null,
            countrySignedResponseContainer
        );
    }
    public EidasAwaitingCycle3DataStateBuilder withCountrySignedResponseContainer(CountrySignedResponseContainer countrySignedResponseContainer) {
        this.countrySignedResponseContainer = countrySignedResponseContainer;
        return this;
    }
    public EidasAwaitingCycle3DataStateBuilder withCountrySignedResponseContainer() {
        this.countrySignedResponseContainer = new CountrySignedResponseContainer(
                "MIEBASE64STRING==", List.of("MIEKEY=="), "http://country.com"
        );
        return this;
    }
}
