package uk.gov.ida.hub.policy.domain;

import java.util.Optional;

public class MatchFromMatchingService extends ResponseFromMatchingService {

    private String matchingServiceAssertion;
    private Optional<LevelOfAssurance> levelOfAssurance;

    @SuppressWarnings("unused")//Needed by JAXB
    private MatchFromMatchingService() {
    }

    public MatchFromMatchingService(String issuer, String inResponseTo, String matchingServiceAssertion, Optional<LevelOfAssurance> levelOfAssurance) {
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
