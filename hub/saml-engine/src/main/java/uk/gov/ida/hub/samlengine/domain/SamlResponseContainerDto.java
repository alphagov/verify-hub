package uk.gov.ida.hub.samlengine.domain;

public class SamlResponseContainerDto {
    private String samlResponse;
    private String authnRequestIssuerId;

    public SamlResponseContainerDto(String samlResponse, String authnRequestIssuerId) {
        this.samlResponse = samlResponse;
        this.authnRequestIssuerId = authnRequestIssuerId;
    }

    protected SamlResponseContainerDto() {
    }

    public String getSamlResponse() {
        return samlResponse;
    }

    public String getAuthnRequestIssuerId() {
        return authnRequestIssuerId;
    }
}
