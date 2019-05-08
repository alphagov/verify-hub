package uk.gov.ida.hub.config.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SelfServiceConfig {

    @Valid
    @JsonProperty
    private boolean enabled = false;

    @Valid
    @JsonProperty
    private URI source;

    @SuppressWarnings("unused")
    public SelfServiceConfig() { }

    public boolean isEnabled() {
        return enabled;
    }

    public URI getSource() {
        return source;
    }
}
