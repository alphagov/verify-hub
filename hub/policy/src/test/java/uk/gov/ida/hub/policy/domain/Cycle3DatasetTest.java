package uk.gov.ida.hub.policy.domain;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class Cycle3DatasetTest {
    private static final String DEFAULT_ATTRIBUTE = "defaultAttribute";
    private static final String DEFAULT_ATTRIBUTE_VALUE = "defaultAttributeValue";
    private Map<String, String> map = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        map.put(DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE_VALUE);
    }

    @Test
    public void getAttributes() throws Exception {
        Cycle3Dataset cycle3Dataset = new Cycle3Dataset(map);
        assertThat(cycle3Dataset.getAttributes()).isEqualTo(map);
    }

    @Test
    public void createFromData() throws Exception {
        Cycle3Dataset cycle3Dataset = Cycle3Dataset.createFromData(DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE_VALUE);
        assertThat(cycle3Dataset.getAttributes()).isEqualTo(map);
    }

    @Test
    public void testToString() throws Exception {
        final Cycle3Dataset cycle3Dataset = Cycle3Dataset.createFromData(DEFAULT_ATTRIBUTE, DEFAULT_ATTRIBUTE_VALUE);
        final StringBuilder sb = new StringBuilder("Cycle3Dataset{");
        sb.append("attributes=").append(cycle3Dataset.getAttributes());
        sb.append('}');

        assertThat(cycle3Dataset.toString()).isEqualTo(sb.toString());
    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(Cycle3Dataset.class).verify();
    }
}
