package uk.gov.ida.hub.samlengine.domain;

public class SamlMessageDto {
    private String samlMessage;

    public SamlMessageDto(String samlMessage) {
        this.samlMessage = samlMessage;
    }

    protected SamlMessageDto() {

    }

    public String getSamlMessage() {
        return samlMessage;
    }

}
