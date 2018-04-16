package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;

import java.net.URI;

public class RequesterErrorStateBuilder {

    private String requestId = "requestId";
    private String authnRequestIssuerEntityId = "authnRequestIssuerEntityId";
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusHours(1);
    private URI assertionConsumerServiceUri = URI.create("assertionConsumerServiceUri");
    private String relayState = "relayState";
    private SessionId sessionId = SessionId.createNewSessionId();
    private Boolean forceAuthentication = false;
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

    public RequesterErrorStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }
}
