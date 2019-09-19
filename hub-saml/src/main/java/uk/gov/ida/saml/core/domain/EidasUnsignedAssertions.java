package uk.gov.ida.saml.core.domain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EidasUnsignedAssertions {
    private String saml;
    private List<String> base64encryptedKeys;

    private EidasUnsignedAssertions() {
    }

    public EidasUnsignedAssertions(String saml, List<String> base64encryptedKeys) {
        this.saml = saml;
        this.base64encryptedKeys = base64encryptedKeys;
    }

    public String getSaml() {
        return saml;
    }

    public List<String> getBase64encryptedKeys() {
        return base64encryptedKeys;
    }
}

