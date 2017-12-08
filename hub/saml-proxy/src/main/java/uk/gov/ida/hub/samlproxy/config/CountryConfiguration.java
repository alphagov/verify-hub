package uk.gov.ida.hub.samlproxy.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class CountryConfiguration {

    @Valid
    @NotNull
    @JsonProperty
    private NullableMetadataConfiguration metadata;

    public CountryConfiguration() {}

    public CountryConfiguration(NullableMetadataConfiguration metadata) {
        this.metadata = metadata;
    }

    public NullableMetadataConfiguration getMetadataConfiguration() {
        return metadata;
    }
}
