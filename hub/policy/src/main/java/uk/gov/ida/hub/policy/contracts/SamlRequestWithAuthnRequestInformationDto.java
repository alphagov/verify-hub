package uk.gov.ida.hub.policy.contracts;

public class SamlRequestWithAuthnRequestInformationDto {
    private final String samlMessage;

    public SamlRequestWithAuthnRequestInformationDto(String samlMessage) {
        this.samlMessage = samlMessage;
    }

    public String getSamlMessage() {
        return samlMessage;
    }
}
