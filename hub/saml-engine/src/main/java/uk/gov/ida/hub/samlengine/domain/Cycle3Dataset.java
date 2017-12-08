package uk.gov.ida.hub.samlengine.domain;

import java.io.Serializable;
import java.util.Map;

public class Cycle3Dataset implements Serializable {
    private Map<String, String> attributes;

    @SuppressWarnings("unused") // needed by JAXB
    private Cycle3Dataset() {}

    public Cycle3Dataset(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

}
