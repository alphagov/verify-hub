package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseFromHubTest {
    private static final String RESPONSE_ID = "responseId";
    private static final String IN_RESPONSE_TO = "inResponseTo";
    private static final String AUTHN_REQUEST_ISSUER_ENTITY_ID = "authnRequestIssuerEntityId";
    private static final List<String> MATCHING_SERVICE_ASSERTION = Collections.singletonList("matchingServiceAssertion");
    private static final Optional<String> RELAY_STATE = Optional.of("relayState");
    private static final URI ASSERTION_CONSUMER_SERVICE_URI = URI.create("assertionConsumerServiceUri");
    private ResponseFromHub responseFromHub;

    @Before
    public void setUp() throws Exception {
        responseFromHub = new ResponseFromHub(
            RESPONSE_ID,
            IN_RESPONSE_TO,
            AUTHN_REQUEST_ISSUER_ENTITY_ID,
            MATCHING_SERVICE_ASSERTION,
            RELAY_STATE,
            ASSERTION_CONSUMER_SERVICE_URI,
            TransactionIdaStatus.Success
        );
    }

    @Test
    public void getAuthnRequestIssuerEntityId() {
        assertThat(responseFromHub.getAuthnRequestIssuerEntityId()).isEqualTo(AUTHN_REQUEST_ISSUER_ENTITY_ID);
    }

    @Test
    public void getResponseId() {
        assertThat(responseFromHub.getResponseId()).isEqualTo(RESPONSE_ID);
    }

    @Test
    public void getInResponseTo() {
        assertThat(responseFromHub.getInResponseTo()).isEqualTo(IN_RESPONSE_TO);
    }

    @Test
    public void getMatchingServiceAssertion() {
        assertThat(responseFromHub.getEncryptedAssertions()).isEqualTo(MATCHING_SERVICE_ASSERTION);
    }

    @Test
    public void getRelayState() {
        assertThat(responseFromHub.getRelayState()).isEqualTo(RELAY_STATE);
    }

    @Test
    public void getAssertionConsumerServiceUri() {
        assertThat(responseFromHub.getAssertionConsumerServiceUri()).isEqualTo(ASSERTION_CONSUMER_SERVICE_URI);
    }

    @Test
    public void getStatus() {
        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.Success);
    }

    @Test
    public void testToString() {
        final StringBuffer sb = new StringBuffer("uk.gov.ida.hub.policy.domain.ResponseFromHub[");
        sb.append("authnRequestIssuerEntityId=").append(responseFromHub.getAuthnRequestIssuerEntityId());
        sb.append(",responseId=").append(responseFromHub.getResponseId());
        sb.append(",inResponseTo=").append(responseFromHub.getInResponseTo());
        sb.append(",status=").append(responseFromHub.getStatus());
        sb.append(",encryptedAssertions=").append(responseFromHub.getEncryptedAssertions());
        sb.append(",relayState=").append(responseFromHub.getRelayState());
        sb.append(",assertionConsumerServiceUri=").append(responseFromHub.getAssertionConsumerServiceUri());
        sb.append(']');

        assertThat(responseFromHub.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ResponseFromHub.class).verify();
    }
}
