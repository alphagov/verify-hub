package uk.gov.ida.hub.policy.contracts;

public class SamlResponseDto {
    private String samlResponse;

    private SamlResponseDto() {
    }
    
    public SamlResponseDto(String samlResponse) {
        this.samlResponse = samlResponse;
    }

    public String getSamlResponse() {
        return samlResponse;
    }
}
