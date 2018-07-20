package uk.gov.ida.hub.samlproxy.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.saml.metadata.EidasMetadataConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryConfiguration {

    @Valid
    @NotNull
    @JsonProperty
    private EidasMetadataConfiguration metadata;

    public CountryConfiguration() {}

    public CountryConfiguration(EidasMetadataConfiguration metadata) {
        this.metadata = metadata;
    }

    public EidasMetadataConfiguration getMetadataConfiguration() {
        return metadata;
    }
}
