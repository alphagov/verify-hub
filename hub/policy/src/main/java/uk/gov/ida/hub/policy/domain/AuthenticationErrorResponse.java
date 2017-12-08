package uk.gov.ida.hub.policy.domain;

public class AuthenticationErrorResponse {

    private String issuer;
    private String principalIpAddressAsSeenByHub;

    @SuppressWarnings("unused")//Needed by JAXB
    private AuthenticationErrorResponse() {
    }

    public AuthenticationErrorResponse(String issuer, String principalIpAddressAsSeenByHub) {
        this.issuer = issuer;
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getPrincipalIpAddressAsSeenByHub() {
        return principalIpAddressAsSeenByHub;
    }
}
