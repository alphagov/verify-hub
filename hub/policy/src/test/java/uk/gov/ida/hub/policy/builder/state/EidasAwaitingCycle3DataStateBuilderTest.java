package uk.gov.ida.hub.policy.builder.state;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class EidasAwaitingCycle3DataStateBuilderTest {
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
    public void anEidasAwaitingCycle3DataState() {
        assertThat(EidasAwaitingCycle3DataStateBuilder.anEidasAwaitingCycle3DataState()).isInstanceOf(EidasAwaitingCycle3DataStateBuilder.class);
    }

    @Test
    public void build() {
        EidasAwaitingCycle3DataState state = EidasAwaitingCycle3DataStateBuilder.anEidasAwaitingCycle3DataState().build();

        assertThat(state.getRequestId()).isEqualTo("requestId");
        assertThat(state.getRequestIssuerEntityId()).isEqualTo("requestIssuerId");
        assertThat(state.getSessionExpiryTimestamp()).isEqualTo(DateTime.now(DateTimeZone.UTC).plusMinutes(10));
        assertThat(state.getAssertionConsumerServiceUri()).isEqualTo(URI.create("assertionConsumerServiceUri"));
        assertThat(state.getSessionId()).isEqualTo(new SessionId("sessionId"));
        assertThat(state.getTransactionSupportsEidas()).isEqualTo(true);
        assertThat(state.getIdentityProviderEntityId()).isEqualTo("identityProviderEntityId");
        assertThat(state.getMatchingServiceEntityId()).isEqualTo("matchingServiceAdapterEntityId");
        assertThat(state.getRelayState()).isEqualTo(Optional.of("relayState"));
        assertThat(state.getPersistentId()).isEqualTo(new PersistentId("nameId"));
        assertThat(state.getLevelOfAssurance()).isEqualTo(LevelOfAssurance.LEVEL_2);
        assertThat(state.getEncryptedIdentityAssertion()).isEqualTo("encryptedIdentityAssertion");
    }
}
