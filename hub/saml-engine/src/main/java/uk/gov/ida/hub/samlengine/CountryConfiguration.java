package uk.gov.ida.hub.samlengine;


import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.samlengine.config.NullableMetadataConfiguration;
import uk.gov.ida.saml.configuration.SamlConfiguration;
import uk.gov.ida.saml.configuration.SamlConfigurationImpl;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class CountryConfiguration {
    @Valid
    @NotNull
    @JsonProperty
    protected SamlConfigurationImpl saml;

    @Valid
    @NotNull
    @JsonProperty
    private NullableMetadataConfiguration metadata;

    public CountryConfiguration() {
    }

    public CountryConfiguration(SamlConfigurationImpl samlConfiguration, NullableMetadataConfiguration metadataConfiguration) {
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
