package uk.gov.ida.hub.policy.contracts;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchingServiceIdaStatus;

// keep in sync with Dto in saml-engine
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

