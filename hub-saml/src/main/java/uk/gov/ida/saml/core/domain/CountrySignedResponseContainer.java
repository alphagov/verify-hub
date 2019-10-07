package uk.gov.ida.saml.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CountrySignedResponseContainer {

    private String base64SamlResponse;
    private String countryEntityId;
    private List<String> base64encryptedKeys;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountrySignedResponseContainer that = (CountrySignedResponseContainer) o;
        return getBase64SamlResponse().equals(that.getBase64SamlResponse()) &&
                getCountryEntityId().equals(that.getCountryEntityId()) &&
                getBase64encryptedKeys().equals(that.getBase64encryptedKeys());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBase64SamlResponse(), getCountryEntityId(), getBase64encryptedKeys());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CountrySignedResponseContainer{");
        sb.append("base64SamlResponse='").append(base64SamlResponse).append('\'');
        sb.append(", countryEntityId='").append(countryEntityId).append('\'');
        sb.append(", base64encryptedKeys=").append(base64encryptedKeys);
        sb.append('}');
        return sb.toString();
    }
}
