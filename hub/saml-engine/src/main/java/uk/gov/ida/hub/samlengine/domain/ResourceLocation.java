package uk.gov.ida.hub.samlengine.domain;

import java.net.URI;

public class ResourceLocation {
    private URI target;

    @SuppressWarnings("unused") // NEEDED BY JAXB
    protected ResourceLocation() {
    }

    public ResourceLocation(URI target) {
        this.target = target;
    }

    public URI getTarget() {
        return target;
    }
}