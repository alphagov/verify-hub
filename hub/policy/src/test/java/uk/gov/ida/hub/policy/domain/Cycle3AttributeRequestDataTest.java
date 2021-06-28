package uk.gov.ida.hub.policy.domain;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class Cycle3AttributeRequestDataTest {
    private static final String CYCLE_3_ATTRIBUTE_NAME = "cycle3AttributeName";
    private static final String REQUEST_ISSUER_ENTITY_ID = "requestIssuerEntityId";
    private static Cycle3AttributeRequestData cycle3AttributeRequestData;

    @BeforeAll
    public static void setUp() {
        cycle3AttributeRequestData = new Cycle3AttributeRequestData(CYCLE_3_ATTRIBUTE_NAME, REQUEST_ISSUER_ENTITY_ID);
    }

    @Test
    public void getAttributeName() {
        assertThat(cycle3AttributeRequestData.getAttributeName()).isEqualTo(CYCLE_3_ATTRIBUTE_NAME);
    }

    @Test
    public void getRequestIssuerId() {
        assertThat(cycle3AttributeRequestData.getRequestIssuerId()).isEqualTo(REQUEST_ISSUER_ENTITY_ID);
    }

    @Test
    public void testToString() {
        final StringBuilder sb = new StringBuilder("Cycle3AttributeRequestData{");
        sb.append("attributeName='").append(cycle3AttributeRequestData.getAttributeName()).append('\'');
        sb.append(", requestIssuerId='").append(cycle3AttributeRequestData.getRequestIssuerId()).append('\'');
        sb.append('}');

        assertThat(cycle3AttributeRequestData.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(Cycle3AttributeRequestData.class).verify();
    }
}
