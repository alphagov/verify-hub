package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

public class OnboardingIdpPredicate implements Predicate<IdentityProviderConfigEntityData> {
    private String transactionEntity;
    private LevelOfAssurance levelOfAssurance;

    public OnboardingIdpPredicate(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        this.transactionEntity = transactionEntity;
        this.levelOfAssurance = levelOfAssurance;
    }

    @Override
    public boolean apply(IdentityProviderConfigEntityData identityProviderConfigEntityData) {
        boolean isOnboarding = levelOfAssurance != null ?
                identityProviderConfigEntityData.isOnboardingAtLoa(levelOfAssurance) :
                identityProviderConfigEntityData.isOnboardingAtAllLevels();

        return !isOnboarding || identityProviderConfigEntityData.getOnboardingTransactionEntityIdsTemp().contains(transactionEntity);
    }

}
