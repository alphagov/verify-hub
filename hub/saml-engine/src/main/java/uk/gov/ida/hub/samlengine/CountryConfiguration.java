package uk.gov.ida.hub.samlengine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.samlengine.config.SamlConfiguration;
import uk.gov.ida.saml.metadata.EidasMetadataConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryConfiguration {
    @Valid
    @NotNull
    @JsonProperty
    protected SamlConfiguration saml;

    @Valid
    @NotNull
    @JsonProperty
    private EidasMetadataConfiguration metadata;

    public CountryConfiguration() {
    }

    public CountryConfiguration(SamlConfiguration samlConfiguration, EidasMetadataConfiguration metadataConfiguration) {
        this.saml = samlConfiguration;
        this.metadata = metadataConfiguration;
    }

    public SamlConfiguration getSamlConfiguration() {
        return saml;
    }

    public EidasMetadataConfiguration getMetadataConfiguration() {
        return metadata;
    }
}
