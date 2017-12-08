package uk.gov.ida.hub.policy.contracts;

import java.net.URI;

public class SamlRequestDto {
    private String samlRequest;
    private URI ssoUri;

    @SuppressWarnings("unused") // needed for JAXB
    private SamlRequestDto() {}

    public SamlRequestDto(String samlRequest, URI ssoUri) {
        this.samlRequest = samlRequest;
        this.ssoUri = ssoUri;
    }

    public String getSamlRequest() {
        return samlRequest;
    }

    public URI getSsoUri() {
        return ssoUri;
    }
}
