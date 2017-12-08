package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.Cycle3Dataset;

import java.util.HashMap;
import java.util.Map;

public class Cycle3DatasetBuilder {

    private Map<String, String> attributes = new HashMap<>();

    public static Cycle3DatasetBuilder aCycle3Dataset() {
        return new Cycle3DatasetBuilder();
    }

    public Cycle3Dataset build() {
        if (!attributes.isEmpty()) {
            attributes.put("test-name", "test-value");
        }

        return new Cycle3Dataset(attributes);
    }
}
