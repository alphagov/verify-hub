package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.AuthenticationErrorResponse;

public class AuthenticationErrorResponseBuilder {

    private String issuer = "issuer";
    private String principalIpAddressAsSeenByHub = "principal-ip-address";

    public static AuthenticationErrorResponseBuilder anAuthenticationErrorResponse() {
        return new AuthenticationErrorResponseBuilder();
    }

    public AuthenticationErrorResponse build() {
        return new AuthenticationErrorResponse(issuer, principalIpAddressAsSeenByHub);
    }

    public AuthenticationErrorResponseBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public AuthenticationErrorResponseBuilder withPrincipalIpAddressAsSeenByHub(String principalIpAddressAsSeenByHub) {
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
        return this;
    }
}
