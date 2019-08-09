package uk.gov.ida.hub.config.validators;

import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;

import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createAbsentOnboardingTransactionConfigException;

public class IdentityProviderConfigOnboardingTransactionValidator {

    private LocalConfigRepository<TransactionConfig> transactionConfigRepository;

    public IdentityProviderConfigOnboardingTransactionValidator(
            final LocalConfigRepository<TransactionConfig> transactionConfigRepository) {

        this.transactionConfigRepository = transactionConfigRepository;
    }

    public void validate(IdentityProviderConfig identityProviderConfig) {
        for (String onboardingTransactionEntityId : identityProviderConfig.getOnboardingTransactionEntityIds()) {
            transactionConfigRepository.getData(onboardingTransactionEntityId)
                    .orElseThrow(() -> createAbsentOnboardingTransactionConfigException(
                        onboardingTransactionEntityId,
                        identityProviderConfig.getEntityId()));
        }
    }
}
