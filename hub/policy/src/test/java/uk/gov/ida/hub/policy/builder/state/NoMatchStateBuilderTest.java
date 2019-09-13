package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class NoMatchStateBuilderTest {
    private static final String RELAY_STATE = "relayState";
    private static final String IDENTITY_PROVIDER_ENTITY_ID = "identityProviderEntityId";
    private static final DateTime NOW = DateTime.now(DateTimeZone.UTC);

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void aNoMatchState() {
        assertThat(NoMatchStateBuilder.aNoMatchState()).isInstanceOf(NoMatchStateBuilder.class);
    }

    @Test
    public void build() {
        NoMatchState noMatchState = NoMatchStateBuilder.aNoMatchState().build();

        assertThat(noMatchState.getRequestId()).isEqualTo("request ID");
        assertThat(noMatchState.getIdentityProviderEntityId()).isEqualTo("idp entity id");
        assertThat(noMatchState.getRequestIssuerEntityId()).isEqualTo("requestIssuerId");
        assertThat(noMatchState.getSessionExpiryTimestamp()).isEqualTo(DateTime.now(DateTimeZone.UTC).plusMinutes(10));
        assertThat(noMatchState.getAssertionConsumerServiceUri()).isEqualTo(URI.create("/someUri"));
        assertThat(noMatchState.getRelayState()).isEqualTo(Optional.empty());
        assertThat(noMatchState.getSessionId()).isEqualTo(new SessionId("sessionId"));
        assertThat(noMatchState.getTransactionSupportsEidas()).isEqualTo(false);
    }

    @Test
    public void withIdentityProviderEntityId() {
        NoMatchState noMatchState = NoMatchStateBuilder.aNoMatchState().withIdentityProviderEntityId(IDENTITY_PROVIDER_ENTITY_ID).build();

        assertThat(noMatchState.getIdentityProviderEntityId()).isEqualTo(IDENTITY_PROVIDER_ENTITY_ID);
    }

    @Test
    public void withRelayState() throws Exception {
        NoMatchState noMatchState = NoMatchStateBuilder.aNoMatchState().withRelayState(RELAY_STATE).build();

        assertThat(noMatchState.getRelayState()).isEqualTo(Optional.of(RELAY_STATE));
    }
}
