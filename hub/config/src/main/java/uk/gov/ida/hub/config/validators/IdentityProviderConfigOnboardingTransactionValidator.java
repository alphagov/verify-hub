package uk.gov.ida.hub.config.validators;


import uk.gov.ida.hub.config.data.ConfigRepository;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;

import java.util.List;

import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createAbsentOnboardingTransactionConfigException;

public class IdentityProviderConfigOnboardingTransactionValidator {

    private ConfigRepository<TransactionConfig> transactionConfigRepository;

    public IdentityProviderConfigOnboardingTransactionValidator(
            final ConfigRepository<TransactionConfig> transactionConfigRepository) {

        this.transactionConfigRepository = transactionConfigRepository;
    }

    public void validate(IdentityProviderConfig identityProviderConfig) {
        List<String> onboardingTransactionEntityIds = identityProviderConfig.getOnboardingTransactionEntityIds();
        for(String onboardingTransactionEntityId : onboardingTransactionEntityIds) {
            if (!transactionConfigRepository.getData(onboardingTransactionEntityId).isPresent()) {
                throw createAbsentOnboardingTransactionConfigException(
                        onboardingTransactionEntityId,
                        identityProviderConfig.getEntityId());
            }
        }
    }
}
