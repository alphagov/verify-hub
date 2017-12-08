package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class EidasAwaitingCycle3DataStateTest {
    private static final String ENCRYPTED_IDENTITY_ASSERTION = "encryptedIdentityAssertion";
    private EidasAwaitingCycle3DataState state;

    @Before
    public void setUp() {
        state = new EidasAwaitingCycle3DataState(
            "requestId",
            "requestIssuerId",
            DateTime.now(),
            URI.create("assertionConsumerServiceUri"),
            new SessionId("sessionId"),
            true,
            "identityProviderEntityId",
            "matchingServiceAdapterEntityId",
            Optional.of("relayState"),
            new PersistentId("persistentId"),
            LevelOfAssurance.LEVEL_2,
            ENCRYPTED_IDENTITY_ASSERTION
        );
    }

    @Test
    public void getEncryptedIdentityAssertion() {
        assertThat(state.getEncryptedIdentityAssertion()).isEqualTo(ENCRYPTED_IDENTITY_ASSERTION);
    }

    @Test
    public void testToString() {
        final StringBuffer sb = new StringBuffer("uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState[");
        sb.append("encryptedIdentityAssertion=").append(state.getEncryptedIdentityAssertion());
        sb.append(",identityProviderEntityId=").append(state.getIdentityProviderEntityId());
        sb.append(",matchingServiceEntityId=").append(state.getMatchingServiceEntityId());
        sb.append(",relayState=").append(state.getRelayState());
        sb.append(",persistentId=").append(state.getPersistentId());
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
        EqualsVerifier.forClass(EidasAwaitingCycle3DataState.class).verify();
    }
}
