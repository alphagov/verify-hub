package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

public class OnboardingIdpPredicate implements Predicate<IdentityProviderConfig> {
    private String transactionEntity;
    private LevelOfAssurance levelOfAssurance;

    public OnboardingIdpPredicate(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        this.transactionEntity = transactionEntity;
        this.levelOfAssurance = levelOfAssurance;
    }

    @Override
    public boolean apply(IdentityProviderConfig identityProviderConfig) {
        boolean isOnboarding = levelOfAssurance != null ?
                identityProviderConfig.isOnboardingAtLoa(levelOfAssurance) :
                identityProviderConfig.isOnboardingAtAllLevels();

        return !isOnboarding || identityProviderConfig.getOnboardingTransactionEntityIdsTemp().contains(transactionEntity);
    }

}
