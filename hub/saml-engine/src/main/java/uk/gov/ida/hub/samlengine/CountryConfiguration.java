package uk.gov.ida.hub.samlengine;


import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.samlengine.config.NullableMetadataConfiguration;
import uk.gov.ida.hub.samlengine.config.SamlConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class CountryConfiguration {
    @Valid
    @NotNull
    @JsonProperty
    protected SamlConfiguration saml;

    @Valid
    @NotNull
    @JsonProperty
    private NullableMetadataConfiguration metadata;

    public CountryConfiguration() {
    }

    public CountryConfiguration(SamlConfiguration samlConfiguration, NullableMetadataConfiguration metadataConfiguration) {
        this.saml = samlConfiguration;
        this.metadata = metadataConfiguration;
    }

    public SamlConfiguration getSamlConfiguration() {
        return saml;
    }

    public NullableMetadataConfiguration getMetadataConfiguration() {
        return metadata;
    }
}
