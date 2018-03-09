package uk.gov.ida.hub.samlengine.builders;

import uk.gov.ida.hub.samlengine.domain.PersistentId;

public class PersistentIdBuilder {

    private String nameId = "default-name-id";

    public static PersistentIdBuilder aPersistentId() {
        return new PersistentIdBuilder();
    }

    public PersistentId buildSamlEnginePersistentId() {
        return new PersistentId(nameId);
    }

    public uk.gov.ida.saml.core.domain.PersistentId buildSamlCorePersistentId() {
        return new uk.gov.ida.saml.core.domain.PersistentId(nameId);
    }

    public PersistentIdBuilder withNameId(String persistentId) {
        this.nameId = persistentId;
        return this;
    }
}
