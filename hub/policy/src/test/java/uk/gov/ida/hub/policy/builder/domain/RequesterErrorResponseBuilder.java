package uk.gov.ida.hub.policy.builder.domain;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.RequesterErrorResponse;

public class RequesterErrorResponseBuilder {

    private String issuer = "issuer";
    private Optional<String> errorMessage = Optional.absent();
    private String principalIpAddressAsSeenByHub = "principal ip address as seen by hub";

    public static RequesterErrorResponseBuilder aRequesterErrorResponse() {
        return new RequesterErrorResponseBuilder();
    }

    public RequesterErrorResponse build() {
        return new RequesterErrorResponse(issuer, errorMessage, principalIpAddressAsSeenByHub);
    }

    public RequesterErrorResponseBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public RequesterErrorResponseBuilder withErrorMessage(String errorMessage) {
        this.errorMessage = Optional.fromNullable(errorMessage);
        return this;
    }

    public RequesterErrorResponseBuilder withPrincipalIpAddressAsSeenByHub(String principalIpAddressAsSeenByHub) {
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
        return this;
    }
}
