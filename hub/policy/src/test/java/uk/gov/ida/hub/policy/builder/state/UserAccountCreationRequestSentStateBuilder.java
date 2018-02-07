package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentStateTransitional;

import java.net.URI;
import java.util.UUID;

import static com.google.common.base.Optional.absent;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

public class UserAccountCreationRequestSentStateBuilder {

    private String requestId = UUID.randomUUID().toString();
    private String identityProviderEntityId = "idp entity id";
    private String requestIssuerId = "request issuer id";
    private Optional<String> relayState = absent();
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_1;
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId sessionId = aSessionId().build();
    private boolean transactionSupportsEidas = false;
    private boolean registering = false;

    public static UserAccountCreationRequestSentStateBuilder aUserAccountCreationRequestSentState() {
        return new UserAccountCreationRequestSentStateBuilder();
    }

    public UserAccountCreationRequestSentStateTransitional buildTransitional() {
        return new UserAccountCreationRequestSentStateTransitional(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                identityProviderEntityId,
                relayState,
                levelOfAssurance,
                registering,
                "matchingServiceEntityId",
                DateTime.now()
        );
    }

    @Deprecated
    public UserAccountCreationRequestSentState build() {
        return new UserAccountCreationRequestSentState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                identityProviderEntityId,
                relayState,
                levelOfAssurance,
                "matchingServiceEntityId"
        );
    }

    public UserAccountCreationRequestSentStateBuilder withRelayState(Optional<String> relayState) {
        this.relayState = relayState;
        return this;
    }

    public UserAccountCreationRequestSentStateBuilder withRegistering(boolean registering) {
        this.registering = registering;
        return this;
    }
}
