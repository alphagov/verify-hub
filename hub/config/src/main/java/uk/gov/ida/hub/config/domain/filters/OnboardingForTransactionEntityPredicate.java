package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

@Deprecated
public class OnboardingForTransactionEntityPredicate implements Predicate<IdentityProviderConfigEntityData> {
    private String transactionEntity;

    public OnboardingForTransactionEntityPredicate(String transactionEntity) {
        this.transactionEntity = transactionEntity;
    }

    @Override
    public boolean apply(IdentityProviderConfigEntityData identityProviderConfigEntityData) {
        boolean isOnboarding = !identityProviderConfigEntityData.getOnboardingTransactionEntityIds().isEmpty();
        return !isOnboarding || identityProviderConfigEntityData.getOnboardingTransactionEntityIds().contains(transactionEntity);
    }

    public String getTransactionEntity() {
        return transactionEntity;
    }
}
