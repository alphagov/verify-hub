package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;

import java.net.URI;
import java.util.UUID;

import static com.google.common.base.Optional.absent;
import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class Cycle3MatchRequestSentStateBuilder {

    private String requestId = UUID.randomUUID().toString();
    private String identityProviderEntityId = "idp entity id";
    private String requestIssuerId = "request issuer id";
    private Optional<String> relayState = absent();
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_1;
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private PersistentId persistentId = aPersistentId().build();
    private String encryptedMatchingDatasetAssertion = "encrypted-matching-dataset-assertion";
    private boolean transactionSupportsEidas = false;

    public static Cycle3MatchRequestSentStateBuilder aCycle3MatchRequestSentState() {
        return new Cycle3MatchRequestSentStateBuilder();
    }

    public Cycle3MatchRequestSentStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Cycle3MatchRequestSentStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public Cycle3MatchRequestSentState build() {
        return new Cycle3MatchRequestSentState(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            identityProviderEntityId,
            relayState,
            levelOfAssurance,
            "matchingServiceEntityId",
            encryptedMatchingDatasetAssertion,
            "aPassthroughAssertion().buildAuthnStatementAssertion()",
            persistentId
        );
    }
}
