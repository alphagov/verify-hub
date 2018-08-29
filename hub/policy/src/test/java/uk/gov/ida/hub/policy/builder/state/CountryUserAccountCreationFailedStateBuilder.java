package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.CountryUserAccountCreationFailedState;

import java.net.URI;

public class CountryUserAccountCreationFailedStateBuilder {

    private String requestId = "requestId";
    private String requestIssuerId = "requestIssuerId";
    private URI assertionConsumerServiceUri = URI.create("/default-service-index");
    private DateTime sessionExpiryTimestamp = DateTime.now(DateTimeZone.UTC).plusMinutes(10);
    private SessionId sessionId = SessionIdBuilder.aSessionId().build();

    public static CountryUserAccountCreationFailedStateBuilder aCountryUserAccountCreationFailedState() {
        return new CountryUserAccountCreationFailedStateBuilder();
    }

    public CountryUserAccountCreationFailedState build() {
        return new CountryUserAccountCreationFailedState(
                requestId,
                requestIssuerId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                Optional.absent(),
                sessionId
        );
    }

    public CountryUserAccountCreationFailedStateBuilder withRequestIssuerId(String requestIssuerId) {
        this.requestIssuerId = requestIssuerId;
        return this;
    }

    public CountryUserAccountCreationFailedStateBuilder withSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public CountryUserAccountCreationFailedStateBuilder withRequestid(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public CountryUserAccountCreationFailedStateBuilder withSessionExpiryTimestamp(DateTime sessionExpiryTimestamp) {
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        return this;
    }
}
