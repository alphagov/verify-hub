package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

@Deprecated
public class OnboardingForTransactionEntityPredicate implements Predicate<IdentityProviderConfig> {
    private String transactionEntity;

    public OnboardingForTransactionEntityPredicate(String transactionEntity) {
        this.transactionEntity = transactionEntity;
    }

    @Override
    public boolean apply(IdentityProviderConfig identityProviderConfig) {
        boolean isOnboarding = !identityProviderConfig.getOnboardingTransactionEntityIds().isEmpty();
        return !isOnboarding || identityProviderConfig.getOnboardingTransactionEntityIds().contains(transactionEntity);
    }

    public String getTransactionEntity() {
        return transactionEntity;
    }
}
