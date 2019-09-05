package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.function.Predicate;

public class SupportedLoaIdpPredicate implements Predicate<IdentityProviderConfig> {
    private LevelOfAssurance levelOfAssurance;

    public SupportedLoaIdpPredicate(LevelOfAssurance levelOfAssurance) {
        this.levelOfAssurance = levelOfAssurance;
    }

    @Override
    public boolean test(IdentityProviderConfig identityProviderConfig) {
        return identityProviderConfig.getSupportedLevelsOfAssurance().contains(levelOfAssurance);
    }
}
