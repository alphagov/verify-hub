package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;

import java.net.URI;

import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;

public class EidasCycle3MatchRequestSentStateBuilder {

    private String encryptedIdentityAssertion = "encryptedIdentityAssertion";
    private PersistentId persistentId = aPersistentId().build();
    private SessionId sessionId = new SessionId("sessionId");
    private String requestId = "requestId";

    public static EidasCycle3MatchRequestSentStateBuilder anEidasCycle3MatchRequestSentState() {
        return new EidasCycle3MatchRequestSentStateBuilder();
    }

    public EidasCycle3MatchRequestSentState build() {
        return new EidasCycle3MatchRequestSentState(
            requestId,
            "requestIssuerId",
            DateTime.now(DateTimeZone.UTC).plusMinutes(10),
            URI.create("assertionConsumerServiceUri"),
            sessionId,
            true,
            "identityProviderEntityId",
            null,
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

    public EidasCycle3MatchRequestSentStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public EidasCycle3MatchRequestSentStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
