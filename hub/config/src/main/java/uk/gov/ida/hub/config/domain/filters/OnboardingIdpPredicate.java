package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.function.Predicate;

public class OnboardingIdpPredicate implements Predicate<IdentityProviderConfig> {
    private String transactionEntity;
    private LevelOfAssurance levelOfAssurance;

    public OnboardingIdpPredicate(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        this.transactionEntity = transactionEntity;
        this.levelOfAssurance = levelOfAssurance;
    }

    @Override
    public boolean test(IdentityProviderConfig identityProviderConfig) {
        boolean isOnboarding = levelOfAssurance != null ?
                identityProviderConfig.isOnboardingAtLoa(levelOfAssurance) :
                identityProviderConfig.isOnboardingAtAllLevels();

        return !isOnboarding || identityProviderConfig.getOnboardingTransactionEntityIdsTemp().contains(transactionEntity);
    }

}
