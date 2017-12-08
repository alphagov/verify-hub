package uk.gov.ida.hub.config.validators;


import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;

import java.util.List;

import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createAbsentOnboardingTransactionConfigException;

public class IdentityProviderConfigOnboardingTransactionValidator {

    private ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigRepository;

    public IdentityProviderConfigOnboardingTransactionValidator(
            final ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigRepository) {

        this.transactionConfigRepository = transactionConfigRepository;
    }

    public void validate(IdentityProviderConfigEntityData identityProviderConfigEntityData) {
        List<String> onboardingTransactionEntityIds = identityProviderConfigEntityData.getOnboardingTransactionEntityIds();
        for(String onboardingTransactionEntityId : onboardingTransactionEntityIds) {
            if (!transactionConfigRepository.getData(onboardingTransactionEntityId).isPresent()) {
                throw createAbsentOnboardingTransactionConfigException(
                        onboardingTransactionEntityId,
                        identityProviderConfigEntityData.getEntityId());
            }
        }
    }
}
