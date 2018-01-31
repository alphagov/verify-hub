package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataStateTransitional;

import java.net.URI;

import static com.google.common.base.Optional.absent;
import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;

public class AwaitingCycle3DataStateBuilder {

    private String requestId = "request-id";
    private Optional<String> relayState = absent();
    private URI assertionConsumerServiceUri = URI.create("/default-service-uri");
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private String transactionEntityId = "transaction entity id";
    private String encryptedMatchingDatasetAssertion = "encrypted-matching-dataset-assertion";
    private boolean transactionSupportsEidas = false;
    private boolean registering = false;

    public static AwaitingCycle3DataStateBuilder anAwaitingCycle3DataState() {
        return new AwaitingCycle3DataStateBuilder();
    }

    public AwaitingCycle3DataStateTransitional build() {
        return new AwaitingCycle3DataStateTransitional(
                requestId,
                "idp entity-id",
                sessionExpiryTimestamp,
                transactionEntityId,
                encryptedMatchingDatasetAssertion,
                "aPassthroughAssertion().buildAuthnStatementAssertion()",
                relayState,
                assertionConsumerServiceUri,
                "matchingServiceEntityId",
                sessionId,
                aPersistentId().build(),
                LevelOfAssurance.LEVEL_1,
                registering,
                transactionSupportsEidas);
    }

    @Deprecated
    public AwaitingCycle3DataState buildOld() {
        return new AwaitingCycle3DataState(
                requestId,
                "idp entity-id",
                sessionExpiryTimestamp,
                transactionEntityId,
                encryptedMatchingDatasetAssertion,
                "aPassthroughAssertion().buildAuthnStatementAssertion()",
                relayState,
                assertionConsumerServiceUri,
                "matchingServiceEntityId",
                sessionId,
                aPersistentId().build(),
                LevelOfAssurance.LEVEL_1,
                transactionSupportsEidas);
    }

    public AwaitingCycle3DataStateBuilder withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public AwaitingCycle3DataStateBuilder withTransactionEntityId(String transactionEntityId) {
        this.transactionEntityId = transactionEntityId;
        return this;
    }

    public AwaitingCycle3DataStateBuilder withSessionExpiryTime(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public AwaitingCycle3DataStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public AwaitingCycle3DataStateBuilder withRegistering(boolean registering) {
        this.registering = registering;
        return this;
    }
}
