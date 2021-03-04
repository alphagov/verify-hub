package uk.gov.ida.hub.policy.domain;

public class AuthnRequestSignInProcess {

    private String requestIssuerId;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthnRequestSignInProcess() {
    }

    public AuthnRequestSignInProcess(String requestIssuerId) {
        this.requestIssuerId = requestIssuerId;
    }

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

}
