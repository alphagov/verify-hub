package uk.gov.ida.hub.policy.domain;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Immutable
public final class Cycle3Dataset implements Serializable {

    private Map<String, String> attributes;

    @SuppressWarnings("unused")//Needed by JAXB
    private Cycle3Dataset() {
    }

    public Cycle3Dataset(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public static Cycle3Dataset createFromData(String attributeKey, String userInputData) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(attributeKey, userInputData);
        return new Cycle3Dataset(attributes);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Cycle3Dataset{");
        sb.append("attributes=").append(attributes);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Cycle3Dataset that = (Cycle3Dataset) o;

        return Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }
}
