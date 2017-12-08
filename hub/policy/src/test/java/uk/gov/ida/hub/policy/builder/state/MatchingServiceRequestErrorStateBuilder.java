package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;

import java.net.URI;

public class MatchingServiceRequestErrorStateBuilder {

    private String requestId = "requestId";
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private String identityProviderEntityId = "identityProviderEntityId";
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(10);
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private boolean transactionSupportsEidas = false;

    public static MatchingServiceRequestErrorStateBuilder aMatchingServiceRequestErrorState() {
        return new MatchingServiceRequestErrorStateBuilder();
    }

    public MatchingServiceRequestErrorState build() {
        return new MatchingServiceRequestErrorState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                identityProviderEntityId,
                Optional.<String>absent(),
                sessionId,
                transactionSupportsEidas);
    }
}
