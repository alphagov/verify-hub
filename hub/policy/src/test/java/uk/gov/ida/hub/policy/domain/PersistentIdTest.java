package uk.gov.ida.hub.policy.domain;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;

public class PersistentIdTest {
    private static final String NAME_ID = "nameId";
    private PersistentId persistentId;

    @Before
    public void setUp() {
        persistentId = aPersistentId().withNameId(NAME_ID).build();
    }

    @Test
    public void getNameId() {
        assertThat(persistentId.getNameId()).isEqualTo(NAME_ID);
    }

    @Test
    public void testToString() {
        final StringBuffer sb = new StringBuffer("PersistentId{");
        sb.append("nameId='").append(NAME_ID).append('\'');
        sb.append('}');
        assertThat(persistentId.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(PersistentId.class).verify();
    }
}
