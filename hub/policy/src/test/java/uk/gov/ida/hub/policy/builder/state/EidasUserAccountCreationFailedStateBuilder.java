package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState;

import java.net.URI;

public class EidasUserAccountCreationFailedStateBuilder {

    private String requestId = "requestId";
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();
    private Boolean forceAuthentication = false;

    public static EidasUserAccountCreationFailedStateBuilder aEidasUserAccountCreationFailedState() {
        return new EidasUserAccountCreationFailedStateBuilder();
    }

    public EidasUserAccountCreationFailedState build() {
        return new EidasUserAccountCreationFailedState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                Optional.absent(),
                sessionId,
                forceAuthentication
        );
    }

    public EidasUserAccountCreationFailedStateBuilder withRequestIssuerId(String requestIssuerId) {
        this.requestIssuerId = requestIssuerId;
        return this;
    }

    public EidasUserAccountCreationFailedStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public EidasUserAccountCreationFailedStateBuilder withRequestid(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public EidasUserAccountCreationFailedStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }

    public EidasUserAccountCreationFailedStateBuilder withForceAuthentication(Boolean forceAuthentication) {
        this.forceAuthentication = forceAuthentication;
        return this;
    }
}
