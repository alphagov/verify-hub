package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.state.NoMatchStateBuilder.aNoMatchState;

public class NoMatchStateTest {
    private static final String IDENTITY_PROVIDER_ENTITY_ID = "identityProviderEntityId";
    private static final Optional<String> RELAY_STATE = Optional.of("relayState");
    private NoMatchState state;

    @Before
    public void setUp() {
        state = aNoMatchState().withIdentityProviderEntityId(IDENTITY_PROVIDER_ENTITY_ID).withRelayState(RELAY_STATE).build();
    }

    @Test
    public void getIdentityProviderEntityId() {
        assertThat(state.getIdentityProviderEntityId()).isEqualTo(IDENTITY_PROVIDER_ENTITY_ID);
    }

    @Test
    public void getRelayState() {
        assertThat(state.getRelayState()).isEqualTo(RELAY_STATE);
    }

    @Test
    public void testToString() {
        final StringBuffer sb = new StringBuffer("NoMatchState{");
        sb.append("identityProviderEntityId='").append(IDENTITY_PROVIDER_ENTITY_ID).append('\'');
        sb.append(", relayState=").append(RELAY_STATE);
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
        EqualsVerifier.forClass(NoMatchState.class).verify();
    }
}
