package uk.gov.ida.integrationtest.hub.policy.rest;

import com.google.common.base.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class EidasCycle3DTOTest {
    private static final DateTime NOW = DateTime.now(DateTimeZone.UTC);
    private static final SessionId SESSION_ID = new SessionId("sessionId");
    private static EidasCycle3DTO eidasCycle3DTO;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
        eidasCycle3DTO = new EidasCycle3DTO(SESSION_ID);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void getSessionId() throws Exception {
        assertThat(eidasCycle3DTO.getSessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    public void getRequestId() throws Exception {
        assertThat(eidasCycle3DTO.getRequestId()).isEqualTo("requestId");
    }

    @Test
    public void getIdentityProviderEntityId() throws Exception {
        assertThat(eidasCycle3DTO.getIdentityProviderEntityId()).isEqualTo("identityProviderEntityId");
    }

    @Test
    public void getSessionExpiryTimestamp() throws Exception {
        assertThat(eidasCycle3DTO.getSessionExpiryTimestamp()).isEqualTo(DateTime.now().plusMinutes(10));
    }

    @Test
    public void getRequestIssuerEntityId() throws Exception {
        assertThat(eidasCycle3DTO.getRequestIssuerEntityId()).isEqualTo("requestIssuerEntityId");
    }

    @Test
    public void getMatchingServiceAssertion() throws Exception {
        assertThat(eidasCycle3DTO.getMatchingServiceAssertion()).isEqualTo("matchingServiceAssertion");
    }

    @Test
    public void getRelayState() throws Exception {
        assertThat(eidasCycle3DTO.getRelayState()).isEqualTo(Optional.of("relayState"));
    }

    @Test
    public void getAssertionConsumerServiceUri() throws Exception {
        assertThat(eidasCycle3DTO.getAssertionConsumerServiceUri()).isEqualTo(URI.create("http://assertionconsumeruri"));
    }

    @Test
    public void getMatchingServiceAdapterEntityId() throws Exception {
        assertThat(eidasCycle3DTO.getMatchingServiceAdapterEntityId()).isEqualTo("matchingServiceAdapterEntityId");
    }

    @Test
    public void getPersistentId() throws Exception {
        assertThat(eidasCycle3DTO.getPersistentId()).isEqualTo(new PersistentId("nameId"));
    }

    @Test
    public void getLevelOfAssurance() throws Exception {
        assertThat(eidasCycle3DTO.getLevelOfAssurance()).isEqualTo(LevelOfAssurance.LEVEL_2);
    }

    @Test
    public void getEncryptedIdentityAssertion() throws Exception {
        assertThat(eidasCycle3DTO.getEncryptedIdentityAssertion()).isEqualTo("encryptedIdentityAssertion");
    }

    @Test
    public void getTransactionSupportsEidas() throws Exception {
        assertThat(eidasCycle3DTO.getTransactionSupportsEidas()).isTrue();
    }

    @Test
    public void testToString() throws Exception {
        final StringBuilder sb = new StringBuilder("EidasCycle3DTO{");
        sb.append("sessionId=").append(eidasCycle3DTO.getSessionId());
        sb.append(", requestId='").append(eidasCycle3DTO.getRequestId()).append('\'');
        sb.append(", identityProviderEntityId='").append(eidasCycle3DTO.getIdentityProviderEntityId()).append('\'');
        sb.append(", sessionExpiryTimestamp=").append(eidasCycle3DTO.getSessionExpiryTimestamp());
        sb.append(", requestIssuerEntityId='").append(eidasCycle3DTO.getRequestIssuerEntityId()).append('\'');
        sb.append(", matchingServiceAssertion='").append(eidasCycle3DTO.getMatchingServiceAssertion()).append('\'');
        sb.append(", relayState=").append(eidasCycle3DTO.getRelayState());
        sb.append(", assertionConsumerServiceUri=").append(eidasCycle3DTO.getAssertionConsumerServiceUri());
        sb.append(", matchingServiceAdapterEntityId='").append(eidasCycle3DTO.getMatchingServiceAdapterEntityId()).append('\'');
        sb.append(", persistentId=").append(eidasCycle3DTO.getPersistentId());
        sb.append(", levelOfAssurance=").append(eidasCycle3DTO.getLevelOfAssurance());
        sb.append(", transactionSupportsEidas=").append(eidasCycle3DTO.getTransactionSupportsEidas());
        sb.append(", encryptedIdentityAssertion='").append(eidasCycle3DTO.getEncryptedIdentityAssertion()).append('\'');
        sb.append('}');

        assertThat(eidasCycle3DTO.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(EidasCycle3DTO.class).verify();
    }
}
