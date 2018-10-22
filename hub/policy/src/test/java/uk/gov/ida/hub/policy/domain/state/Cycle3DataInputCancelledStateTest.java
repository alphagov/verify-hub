package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class Cycle3DataInputCancelledStateTest {
    private static final Optional<String> RELAY_STATE = Optional.of("relayState");
    private Cycle3DataInputCancelledState state;

    @Before
    public void setUp() throws Exception {
        state = new Cycle3DataInputCancelledState(
            "request",
            DateTime.now(),
            RELAY_STATE,
            "requestIssuerEntityId",
            URI.create("assertionConsumerServiceUri"),
            new SessionId("sessionId"),
            true
        );
    }

    @Test
    public void getRelayState() throws Exception {
        assertThat(state.getRelayState()).isEqualTo(RELAY_STATE);
    }

    @Test
    public void testToString() throws Exception {
        final StringBuffer sb = new StringBuffer("Cycle3DataInputCancelledState{");
        sb.append("relayState=").append(state.getRelayState());
        sb.append(", requestId='").append(state.getRequestId()).append('\'');
        sb.append(", sessionId=").append(state.getSessionId());
        sb.append(", requestIssuerEntityId='").append(state.getRequestIssuerEntityId()).append('\'');
        sb.append(", sessionExpiryTimestamp=").append(state.getSessionExpiryTimestamp());
        sb.append(", assertionConsumerServiceUri=").append(state.getAssertionConsumerServiceUri());
        sb.append(", transactionSupportsEidas=").append(state.getTransactionSupportsEidas());
        sb.append('}');

        assertThat(state.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(Cycle3DataInputCancelledState.class).withIgnoredFields("forceAuthentication").verify();
    }
}
