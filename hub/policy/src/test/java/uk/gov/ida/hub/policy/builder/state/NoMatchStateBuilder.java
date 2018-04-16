package uk.gov.ida.hub.policy.builder.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;

import java.net.URI;

public class NoMatchStateBuilder {
    private String identityProviderEntityId = "idp entity id";
    private Optional<String> relayState = Optional.absent();

    public static NoMatchStateBuilder aNoMatchState() {
        return new NoMatchStateBuilder();
    }

    public NoMatchState build() {
        return new NoMatchState(
            "request ID",
            identityProviderEntityId,
            "requestIssuerId",
            DateTime.now(DateTimeZone.UTC).plusMinutes(10),
            URI.create("/someUri"),
            relayState,
            new SessionId("sessionId"),
            false);
    }

    public NoMatchStateBuilder withIdentityProviderEntityId(final String identityProviderEntityId) {
        this.identityProviderEntityId = identityProviderEntityId;
        return this;
    }

    public NoMatchStateBuilder withRelayState(final Optional<String> relayState) {
        this.relayState = relayState;
        return this;
    }
}
