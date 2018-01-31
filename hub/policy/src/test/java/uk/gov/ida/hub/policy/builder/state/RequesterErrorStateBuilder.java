package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorStateTransitional;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class RequesterErrorStateBuilder {

    private String requestId = "requestId";
    private String authnRequestIssuerEntityId = "authnRequestIssuerEntityId";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusHours(1);
    private URI assertionConsumerServiceUri = URI.create("assertionConsumerServiceUri");
    private Optional<String> relayState = Optional.of("relayState");
    private SessionId sessionId = SessionId.createNewSessionId();
    private Optional<Boolean> forceAuthentication = Optional.of(false);
    private List<String> availableIdentityProviderEntityIds = Collections.emptyList();
    private boolean transactionSupportsEidas = false;

    public static RequesterErrorStateBuilder aRequesterErrorState() {
        return new RequesterErrorStateBuilder();
    }

    public RequesterErrorStateTransitional buildTransitional() {
        return new RequesterErrorStateTransitional(
                requestId,
                authnRequestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                relayState,
                sessionId,
                forceAuthentication,
                transactionSupportsEidas);
    }

    @Deprecated
    public RequesterErrorState build() {
        return new RequesterErrorState(
                requestId,
                authnRequestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                relayState,
                sessionId,
                forceAuthentication,
                availableIdentityProviderEntityIds,
                transactionSupportsEidas);
    }
}
