package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;

import java.net.URI;

import static com.google.common.base.Optional.absent;

public class UserAccountCreatedStateBuilder {

    private String requestId = "request-id";
    private URI assertionConsumerServiceUri = URI.create("http://assertionconsumeruri");
    private String requestIssuerId = "request issuer id";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private String identityProviderEntityId = "identity-provider-id";
    private String matchingServiceAssertion = "aPassthroughAssertion().buildMatchingServiceAssertion()";
    private Optional<String> relayState = absent();
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private boolean transactionSupportsEidas = false;
    private boolean registering = false;

    public static UserAccountCreatedStateBuilder aUserAccountCreatedState() {
        return new UserAccountCreatedStateBuilder();
    }

    public UserAccountCreatedState build() {
        return new UserAccountCreatedState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                identityProviderEntityId,
                matchingServiceAssertion,
                relayState,
                levelOfAssurance,
                registering,
                transactionSupportsEidas);
    }

    public UserAccountCreatedStateBuilder withIdentityProviderEntityId(final String identityProviderEntityId) {
        this.identityProviderEntityId = identityProviderEntityId;
        return this;
    }

    public UserAccountCreatedStateBuilder withRelayState(Optional<String> relayState) {
        this.relayState = relayState;
        return this;
    }

    public UserAccountCreatedStateBuilder withRegistering(boolean registering) {
        this.registering = registering;
        return this;
    }
}
