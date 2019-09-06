package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;

import java.util.Optional;

public class MatchFromMatchingServiceBuilder {

    private String issuer = "default issuer";
    private String inResponseTo = "default in response to";
    private String matchingServiceAssertion = "aPassthroughAssertion().buildAuthnStatementAssertion()";
    private Optional<LevelOfAssurance> levelOfAssurance = Optional.empty();

    public static MatchFromMatchingServiceBuilder aMatchFromMatchingService() {
        return new MatchFromMatchingServiceBuilder();
    }

    public MatchFromMatchingService build() {
        return new MatchFromMatchingService(issuer, inResponseTo, matchingServiceAssertion, levelOfAssurance);
    }


    public MatchFromMatchingServiceBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public MatchFromMatchingServiceBuilder withInResponseTo(String inResponseTo) {
        this.inResponseTo = inResponseTo;
        return this;
    }
}
