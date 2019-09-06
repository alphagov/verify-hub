package uk.gov.ida.hub.policy.domain;

import java.util.Optional;

public class UserAccountCreatedFromMatchingService extends ResponseFromMatchingService {
    private String matchingServiceAssertion;
    private Optional<LevelOfAssurance> levelOfAssurance;

    @SuppressWarnings("unused")//Needed by JAXB
    private UserAccountCreatedFromMatchingService() {
    }

    public UserAccountCreatedFromMatchingService(final String issuer, final String inResponseTo, final String matchingServiceAssertion, Optional<LevelOfAssurance> levelOfAssurance) {
        super(issuer, inResponseTo);
        this.matchingServiceAssertion = matchingServiceAssertion;
        this.levelOfAssurance = levelOfAssurance;
    }

    public String getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public Optional<LevelOfAssurance> getLevelOfAssurance() {
        return levelOfAssurance;
    }
}
