package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

public class SupportedLoaIdpPredicate implements com.google.common.base.Predicate<IdentityProviderConfigEntityData> {
    private LevelOfAssurance levelOfAssurance;

    public SupportedLoaIdpPredicate(LevelOfAssurance levelOfAssurance) {
        this.levelOfAssurance = levelOfAssurance;
    }

    @Override
    public boolean apply(IdentityProviderConfigEntityData identityProviderConfigEntityData) {
        return identityProviderConfigEntityData.getSupportedLevelsOfAssurance().contains(levelOfAssurance);
    }
}
