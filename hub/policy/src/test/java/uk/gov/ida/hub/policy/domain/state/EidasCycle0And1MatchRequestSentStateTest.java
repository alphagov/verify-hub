package uk.gov.ida.hub.policy.domain.state;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.PersistentId;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.state.EidasCycle0And1MatchRequestSentStateBuilder.anEidasCycle0And1MatchRequestSentState;

public class EidasCycle0And1MatchRequestSentStateTest {
    private static final String ENCRYPTED_IDENTITY_ASSERTION = "encryptedIdentityAssertion";
    private static final PersistentId PERSISTENT_ID = new PersistentId("persistentId");
    private EidasCycle0And1MatchRequestSentState state;

    @Before
    public void setUp() {
        state = anEidasCycle0And1MatchRequestSentState().withEncryptedIdentityAssertion(ENCRYPTED_IDENTITY_ASSERTION).withPersistentId(PERSISTENT_ID).build();
    }

    @Test
    public void getEncryptedIdentityAssertion() {
        assertThat(state.getEncryptedIdentityAssertion()).isEqualTo(ENCRYPTED_IDENTITY_ASSERTION);
    }

    @Test
    public void getPersistentId() {
        assertThat(state.getPersistentId()).isEqualTo(PERSISTENT_ID);
    }

    @Test
    public void testToString() {
        final StringBuffer sb = new StringBuffer("EidasCycle0And1MatchRequestSentState{");
        sb.append("encryptedIdentityAssertion='").append(ENCRYPTED_IDENTITY_ASSERTION).append('\'');
        sb.append(", persistentId=").append(PERSISTENT_ID);
        sb.append(", identityProviderEntityId='").append(state.getIdentityProviderEntityId()).append('\'');
        sb.append(", relayState=").append(state.getRelayState());
        sb.append(", requestSentTime=").append(state.getRequestSentTime());
        sb.append(", idpLevelOfAssurance=").append(state.getIdpLevelOfAssurance());
        sb.append(", matchingServiceEntityId='").append(state.getMatchingServiceAdapterEntityId()).append('\'');
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
        EqualsVerifier.forClass(EidasCycle0And1MatchRequestSentState.class).verify();
    }
}
