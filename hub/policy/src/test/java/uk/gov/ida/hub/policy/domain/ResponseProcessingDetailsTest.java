package uk.gov.ida.hub.policy.domain;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseProcessingDetailsTest {
    private static final SessionId SESSION_ID = new SessionId("sessionId");
    private static final String TRANSACTION_ENTITY_ID = "transactionEntityId";
    private static ResponseProcessingDetails responseProcessingDetails;

    @BeforeAll
    public static void setUp() {
        responseProcessingDetails = new ResponseProcessingDetails(
            SESSION_ID,
            ResponseProcessingStatus.GET_C3_DATA,
            TRANSACTION_ENTITY_ID
        );
    }

    @Test
    public void getSessionId() {
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    public void getResponseProcessingStatus() {
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.GET_C3_DATA);
    }

    @Test
    public void getTransactionEntityId() {
        assertThat(responseProcessingDetails.getTransactionEntityId()).isEqualTo(TRANSACTION_ENTITY_ID);
    }

    @Test
    public void testToString() {
        final StringBuilder sb = new StringBuilder("ResponseProcessingDetails{");
        sb.append("sessionId=").append(responseProcessingDetails.getSessionId());
        sb.append(", responseProcessingStatus=").append(responseProcessingDetails.getResponseProcessingStatus());
        sb.append(", transactionEntityId='").append(responseProcessingDetails.getTransactionEntityId()).append('\'');
        sb.append('}');

        assertThat(responseProcessingDetails.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ResponseProcessingDetails.class).verify();
    }
}
