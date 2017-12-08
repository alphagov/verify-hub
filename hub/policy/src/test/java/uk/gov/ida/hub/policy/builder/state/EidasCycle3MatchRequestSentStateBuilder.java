package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;

import java.net.URI;

import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;

public class EidasCycle3MatchRequestSentStateBuilder {

    private String encryptedIdentityAssertion = "encryptedIdentityAssertion";
    private PersistentId persistentId = aPersistentId().build();

    public static EidasCycle3MatchRequestSentStateBuilder anEidasCycle3MatchRequestSentState() {
        return new EidasCycle3MatchRequestSentStateBuilder();
    }

    public EidasCycle3MatchRequestSentState build() {
        return new EidasCycle3MatchRequestSentState(
            "requestId",
            "requestIssuerId",
            DateTime.now().plusMinutes(10),
            URI.create("assertionConsumerServiceUri"),
            new SessionId("sessionId"),
            true,
            "identityProviderEntityId",
            Optional.<String>absent(),
            LevelOfAssurance.LEVEL_2,
            "matchingServiceAdapterEntityId",
            encryptedIdentityAssertion,
            persistentId
        );
    }

    public EidasCycle3MatchRequestSentStateBuilder withEncryptedIdentityAssertion(final String encryptedIdentityAssertion) {
        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
        return this;
    }

    public EidasCycle3MatchRequestSentStateBuilder withPersistentId(final PersistentId persistentId) {
        this.persistentId = persistentId;
        return this;
    }
}
