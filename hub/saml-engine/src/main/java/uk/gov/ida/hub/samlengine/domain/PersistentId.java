package uk.gov.ida.hub.samlengine.domain;

import java.io.Serializable;

public class PersistentId implements Serializable {

    private String nameId;

    @SuppressWarnings("unused") // needed for JAXB
    private PersistentId() {}

    public PersistentId(String nameId) {
        this.nameId = nameId;
    }

    public String getNameId() {
        return nameId;
    }
}
