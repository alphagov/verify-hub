package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryConfig implements EntityIdentifiable {
    @SuppressWarnings("unused") // needed to prevent guice injection
    protected CountryConfig() {
    }

    @Valid
    @NotNull
    @JsonProperty
    protected String entityId;

    @Valid
    @NotNull
    @JsonProperty
    protected String simpleId;

    @Valid
    @NotNull
    @JsonProperty
    protected boolean enabled;

    @Valid
    @JsonProperty
    protected String overriddenSsoUrl;

    @Override
    public String getEntityId() {
        return entityId;
    }

    public String getSimpleId() {
        return simpleId;
    }

    public boolean getEnabled() { return enabled; }

    public String getOverriddenSsoUrl() { return overriddenSsoUrl; }
}
