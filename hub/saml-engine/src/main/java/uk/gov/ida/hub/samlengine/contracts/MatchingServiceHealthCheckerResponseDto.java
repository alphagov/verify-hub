package uk.gov.ida.hub.samlengine.contracts;

import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;

public class MatchingServiceHealthCheckerResponseDto {
    private MatchingServiceIdaStatus status;
    private String inResponseTo;
    private String issuer;
    private String id;

    private MatchingServiceHealthCheckerResponseDto() {}

    public MatchingServiceHealthCheckerResponseDto(MatchingServiceIdaStatus status, String inResponseTo, String issuer, String id) {
        this.status = status;
        this.inResponseTo = inResponseTo;
        this.issuer = issuer;
        this.id = id;
    }

    public MatchingServiceIdaStatus getStatus() {
        return status;
    }

    public String getInResponseTo() {
        return inResponseTo;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getId() {
        return id;
    }
}
