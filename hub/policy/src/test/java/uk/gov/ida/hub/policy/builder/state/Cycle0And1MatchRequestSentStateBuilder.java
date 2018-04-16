package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;

import java.net.URI;

import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class Cycle0And1MatchRequestSentStateBuilder {

    private String matchingServiceEntityId = "matching-service-entityId";
    private String requestIssuerId = "request-issuer-id";
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private PersistentId persistentId = aPersistentId().build();
    private String requestId = "requestId";
    private String encryptedMatchingDatasetAssertion = "encrypted-matching-dataset-assertion";
    private boolean transactionSupportsEidas = false;
    private boolean registering = false;

    public static Cycle0And1MatchRequestSentStateBuilder aCycle0And1MatchRequestSentState() {
        return new Cycle0And1MatchRequestSentStateBuilder();
    }

    public Cycle0And1MatchRequestSentState build() {
        return new Cycle0And1MatchRequestSentState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                URI.create("default-service-uri"),
                sessionId,
                transactionSupportsEidas,
                registering,
                "idp-entity-id",
                null,
                LevelOfAssurance.LEVEL_1,
                matchingServiceEntityId,
                encryptedMatchingDatasetAssertion,
                "aPassthroughAssertion().buildAuthnStatementAssertion()",
                persistentId
        );
    }

    public Cycle0And1MatchRequestSentStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public Cycle0And1MatchRequestSentStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Cycle0And1MatchRequestSentStateBuilder withMatchingServiceEntityId(String matchingServiceEntityId) {
        this.matchingServiceEntityId = matchingServiceEntityId;
        return this;
    }

    public Cycle0And1MatchRequestSentStateBuilder withRequestIssuerEntityId(String requestIssuerEntityId) {
        this.requestIssuerId = requestIssuerEntityId;
        return this;
    }

    public Cycle0And1MatchRequestSentStateBuilder withRegistering(boolean registering) {
        this.registering = registering;
        return this;
    }
}
