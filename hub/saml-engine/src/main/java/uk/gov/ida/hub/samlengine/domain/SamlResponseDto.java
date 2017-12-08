package uk.gov.ida.hub.samlengine.domain;

public class SamlResponseDto {
    private String samlResponse;

    public SamlResponseDto(String samlResponse) {
        this.samlResponse = samlResponse;
    }

    protected SamlResponseDto() {

    }

    public String getSamlResponse() {
        return samlResponse;
    }

}
