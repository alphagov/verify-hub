package uk.gov.ida.hub.policy.domain.state;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.PersistentId;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.state.EidasCycle3MatchRequestSentStateBuilder.anEidasCycle3MatchRequestSentState;

public class EidasCycle3MatchRequestSentStateTest {
    private static final String ENCRYPTED_IDENTITY_ASSERTION = "encryptedIdentityAssertion";
    private static final PersistentId PERSISTENT_ID = new PersistentId("persistentId");
    private EidasCycle3MatchRequestSentState state;

    @Before
    public void setUp() {
        state = anEidasCycle3MatchRequestSentState().withEncryptedIdentityAssertion(ENCRYPTED_IDENTITY_ASSERTION).withPersistentId(PERSISTENT_ID).build();
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
        final StringBuffer sb = new StringBuffer("EidasCycle3MatchRequestSentState{");
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
        EqualsVerifier.forClass(EidasCycle3MatchRequestSentState.class)
                .withIgnoredFields("encryptedIdentityAssertion", "persistentId")
                .withRedefinedSuperclass()
                .usingGetClass()
                .verify();
    }
}
