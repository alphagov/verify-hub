package uk.gov.ida.hub.policy.domain;

public class AuthnRequestSignInDetailsDto {

    private String requestIssuerId;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthnRequestSignInDetailsDto() { }

    public AuthnRequestSignInDetailsDto(
            String requestIssuerId) {

        this.requestIssuerId = requestIssuerId;
    }

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

}
