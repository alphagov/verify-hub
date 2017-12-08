package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;

public class RequesterErrorResponse {

    private String issuer;
    private Optional<String> errorMessage;
    private String principalIpAddressAsSeenByHub;

    @SuppressWarnings("unused")//Needed by JAXB
    private RequesterErrorResponse() {
    }

    public RequesterErrorResponse(String issuer, Optional<String> errorMessage, String principalIpAddressAsSeenByHub) {
        this.issuer = issuer;
        this.errorMessage = errorMessage;
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
    }

    public String getIssuer() {
        return issuer;
    }

    public Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public String getPrincipalIpAddressAsSeenByHub() {
        return principalIpAddressAsSeenByHub;
    }
}
