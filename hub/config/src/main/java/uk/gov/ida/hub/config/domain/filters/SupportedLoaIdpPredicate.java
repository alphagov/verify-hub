package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

public class SupportedLoaIdpPredicate implements com.google.common.base.Predicate<IdentityProviderConfig> {
    private LevelOfAssurance levelOfAssurance;

    public SupportedLoaIdpPredicate(LevelOfAssurance levelOfAssurance) {
        this.levelOfAssurance = levelOfAssurance;
    }

    @Override
    public boolean apply(IdentityProviderConfig identityProviderConfig) {
        return identityProviderConfig.getSupportedLevelsOfAssurance().contains(levelOfAssurance);
    }
}
