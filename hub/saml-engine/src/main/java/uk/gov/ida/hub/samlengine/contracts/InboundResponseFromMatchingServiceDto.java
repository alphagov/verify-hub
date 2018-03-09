package uk.gov.ida.hub.samlengine.contracts;

import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;

import java.util.Optional;

// keep in sync with Dto in policy
public class InboundResponseFromMatchingServiceDto {
    private MatchingServiceIdaStatus status;
    private String inResponseTo;
    private String issuer;
    private Optional<String> underlyingMatchingServiceAssertionBlob;
    private Optional<LevelOfAssurance> levelOfAssurance;

    protected InboundResponseFromMatchingServiceDto() {}

    public InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus status,
                                                 String inResponseTo,
                                                 String issuer,
                                                 Optional<String> underlyingMatchingServiceAssertionBlob,
                                                 Optional<LevelOfAssurance> levelOfAssurance) {
        this.status = status;
        this.inResponseTo = inResponseTo;
        this.issuer = issuer;
        this.underlyingMatchingServiceAssertionBlob = underlyingMatchingServiceAssertionBlob;
        this.levelOfAssurance = levelOfAssurance;
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

    public Optional<String> getUnderlyingMatchingServiceAssertionBlob() {
        return underlyingMatchingServiceAssertionBlob;
    }

    public Optional<LevelOfAssurance> getLevelOfAssurance() {
        return levelOfAssurance;
    }
}
