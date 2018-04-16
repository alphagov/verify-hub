package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;

import java.net.URI;

import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;

public class EidasCycle0And1MatchRequestSentStateBuilder {

    private String encryptedIdentityAssertion = "encryptedIdentityAssertion";
    private PersistentId persistentId = aPersistentId().build();

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
            persistentId
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
}
