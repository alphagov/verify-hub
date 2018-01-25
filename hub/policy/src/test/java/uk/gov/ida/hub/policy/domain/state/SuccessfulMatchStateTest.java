package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class SuccessfulMatchStateTest {
    private static final String IDENTITY_PROVIDER_ENTITY_ID = "identityProviderEntityId";
    private static final String MATCHING_SERVICE_ASSERTION = "matchingServiceAssertion";
    private static final Optional<String> RELAY_STATE = Optional.of("relayState");
    private static final LevelOfAssurance LEVEL_OF_ASSURANCE = LevelOfAssurance.LEVEL_2;
    private static final boolean IS_REGISTERING = false;
    private SuccessfulMatchState state;

    @Before
    public void setUp() {
        state = new SuccessfulMatchState(
            "requestId",
            DateTime.now(),
            IDENTITY_PROVIDER_ENTITY_ID,
            MATCHING_SERVICE_ASSERTION,
            RELAY_STATE,
            "requestIssuerId",
            URI.create("assertionConsumerServiceUri"),
            new SessionId("sessionId"),
            LEVEL_OF_ASSURANCE,
                IS_REGISTERING,
            false
        );
    }

    @Test
    public void getIdentityProviderEntityId() {
        assertThat(state.getIdentityProviderEntityId()).isEqualTo(IDENTITY_PROVIDER_ENTITY_ID);
    }

    @Test
    public void getMatchingServiceAssertion() {
        assertThat(state.getMatchingServiceAssertion()).isEqualTo(MATCHING_SERVICE_ASSERTION);
    }

    @Test
    public void getRelayState() {
        assertThat(state.getRelayState()).isEqualTo(RELAY_STATE);
    }

    @Test
    public void getLevelOfAssurance() {
        assertThat(state.getLevelOfAssurance()).isEqualTo(LEVEL_OF_ASSURANCE);
    }

    @Test
    public void getIsRegistering() {
        assertThat(state.isRegistering()).isEqualTo(IS_REGISTERING);
    }

    @Test
    public void testToString() {
        final StringBuffer sb = new StringBuffer("uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState[");
        sb.append("isRegistering=").append(state.isRegistering());
        sb.append(",identityProviderEntityId=").append(state.getIdentityProviderEntityId());
        sb.append(",matchingServiceAssertion=").append(state.getMatchingServiceAssertion());
        sb.append(",relayState=").append(state.getRelayState());
        sb.append(",levelOfAssurance=").append(state.getLevelOfAssurance());
        sb.append(",requestId=").append(state.getRequestId());
        sb.append(",requestIssuerEntityId=").append(state.getRequestIssuerEntityId());
        sb.append(",sessionExpiryTimestamp=").append(state.getSessionExpiryTimestamp());
        sb.append(",assertionConsumerServiceUri=").append(state.getAssertionConsumerServiceUri());
        sb.append(",sessionId=").append(state.getSessionId());
        sb.append(",transactionSupportsEidas=").append(state.getTransactionSupportsEidas());
        sb.append(']');

        assertThat(state.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(SuccessfulMatchState.class).withRedefinedSuperclass().verify();
    }
}
