package uk.gov.ida.hub.samlsoapproxy.builders;

import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;

public class MatchingServiceHealthCheckerResponseDtoBuilder {

    private MatchingServiceIdaStatus status;
    private String inResponseTo = "inResponseTo";
    private String issuerId = "issuerId";
    private String id = "id";

    public static MatchingServiceHealthCheckerResponseDtoBuilder anInboundResponseFromMatchingServiceDto() {
        return new MatchingServiceHealthCheckerResponseDtoBuilder();
    }

    public MatchingServiceHealthCheckerResponseDto build() {
        return new MatchingServiceHealthCheckerResponseDto(
                status,
                inResponseTo,
                issuerId,
                id);
    }

    public MatchingServiceHealthCheckerResponseDtoBuilder withStatus(MatchingServiceIdaStatus status) {
        this.status = status;
        return this;
    }

    public MatchingServiceHealthCheckerResponseDtoBuilder withIssuer(String issuerId) {
        this.issuerId = issuerId;
        return this;
    }

    public MatchingServiceHealthCheckerResponseDtoBuilder withInResponseTo(String inResponseTo) {
        this.inResponseTo = inResponseTo;
        return this;
    }

    public MatchingServiceHealthCheckerResponseDtoBuilder withId() {
        this.id = id;
        return this;
    }
}
