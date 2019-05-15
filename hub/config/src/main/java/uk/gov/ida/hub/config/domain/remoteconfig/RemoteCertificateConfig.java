package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class RemoteCertificateConfig {

    @JsonProperty
    protected String name;

    @JsonProperty
    protected String value;

    @SuppressWarnings("unused")
    protected RemoteCertificateConfig() {
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
