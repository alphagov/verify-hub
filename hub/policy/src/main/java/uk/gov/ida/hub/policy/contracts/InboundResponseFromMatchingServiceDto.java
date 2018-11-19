package uk.gov.ida.hub.policy.contracts;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchingServiceIdaStatus;

// keep in sync with Dto in saml-engine
public class InboundResponseFromMatchingServiceDto {
    private MatchingServiceIdaStatus status;
    private String inResponseTo;
    private String issuer;
    private Optional<String> encryptedMatchingServiceAssertion;
    private Optional<LevelOfAssurance> levelOfAssurance;

    protected InboundResponseFromMatchingServiceDto() {}

    public InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus status,
                                                 String inResponseTo,
                                                 String issuer,
                                                 Optional<String> encryptedMatchingServiceAssertion,
                                                 Optional<LevelOfAssurance> levelOfAssurance) {
        this.status = status;
        this.inResponseTo = inResponseTo;
        this.issuer = issuer;
        this.encryptedMatchingServiceAssertion = encryptedMatchingServiceAssertion;
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

    public Optional<String> getEncryptedMatchingServiceAssertion() {
        return encryptedMatchingServiceAssertion;
    }

    public Optional<LevelOfAssurance> getLevelOfAssurance() {
        return levelOfAssurance;
    }
}

