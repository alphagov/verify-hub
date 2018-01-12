package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;

import java.net.URI;

public class RequesterErrorStateBuilder {

    private String requestId = "requestId";
    private String authnRequestIssuerEntityId = "authnRequestIssuerEntityId";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusHours(1);
    private URI assertionConsumerServiceUri = URI.create("assertionConsumerServiceUri");
    private Optional<String> relayState = Optional.of("relayState");
    private SessionId sessionId = SessionId.createNewSessionId();
    private Optional<Boolean> forceAuthentication = Optional.of(false);
    private boolean transactionSupportsEidas = false;

    public static RequesterErrorStateBuilder aRequesterErrorState() {
        return new RequesterErrorStateBuilder();
    }

    public RequesterErrorState build() {
        return new RequesterErrorState(
            requestId,
            authnRequestIssuerEntityId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            relayState,
            sessionId,
            forceAuthentication,
            transactionSupportsEidas);
    }

}
