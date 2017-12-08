package uk.gov.ida.hub.samlengine.contracts;

public class SamlRequestWithAuthnRequestInformationDto {
    private String samlMessage;

    @SuppressWarnings("unused") // needed for JAXB
    private SamlRequestWithAuthnRequestInformationDto(){}

    public SamlRequestWithAuthnRequestInformationDto(final String samlMessage) {
        this.samlMessage = samlMessage;
    }

    public String getSamlMessage() {
        return samlMessage;
    }
}
