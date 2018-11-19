package uk.gov.ida.hub.policy.contracts;

public class SamlResponseContainerDto {
    private String samlResponse;
    private String authnRequestIssuerId;

    private SamlResponseContainerDto() {
    }

    public SamlResponseContainerDto(String samlResponse, String authnRequestIssuerId) {
        this.samlResponse = samlResponse;
        this.authnRequestIssuerId = authnRequestIssuerId;
    }

    public String getSamlResponse() {
        return samlResponse;
    }

    public String getAuthnRequestIssuerId() {
        return authnRequestIssuerId;
    }
}
