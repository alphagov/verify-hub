package uk.gov.ida.saml.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CountrySignedResponseContainer {

    private String base64SamlResponse;
    private List<String> base64encryptedKeys;
    private String countryEntityId;

    @SuppressWarnings("unused")
    private CountrySignedResponseContainer() {
        // needed for JAXB
    }

    public CountrySignedResponseContainer(String base64SamlResponse, List<String> base64encryptedKeys, String countryEntityId) {
        this.base64SamlResponse = base64SamlResponse;
        this.base64encryptedKeys = base64encryptedKeys;
        this.countryEntityId = countryEntityId;
    }

    public String getBase64SamlResponse() {
        return base64SamlResponse;
    }

    public String getCountryEntityId() {
        return countryEntityId;
    }

    public List<String> getBase64encryptedKeys() {
        return base64encryptedKeys;
    }
}
