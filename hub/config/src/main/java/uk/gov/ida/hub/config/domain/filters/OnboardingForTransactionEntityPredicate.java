package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import java.util.function.Predicate;

@Deprecated
public class OnboardingForTransactionEntityPredicate implements Predicate<IdentityProviderConfig> {
    private String transactionEntity;

    public OnboardingForTransactionEntityPredicate(String transactionEntity) {
        this.transactionEntity = transactionEntity;
    }

    @Override
    public boolean test(IdentityProviderConfig identityProviderConfig) {
        boolean isOnboarding = !identityProviderConfig.getOnboardingTransactionEntityIds().isEmpty();
        return !isOnboarding || identityProviderConfig.getOnboardingTransactionEntityIds().contains(transactionEntity);
    }

    public String getTransactionEntity() {
        return transactionEntity;
    }
}
