package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteCertificateConfig {

    @JsonProperty
    protected String id;

    @JsonProperty
    protected String name;

    @JsonProperty
    protected String value;

    @SuppressWarnings("unused")
    public RemoteCertificateConfig() {
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
