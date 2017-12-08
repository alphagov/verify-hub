package uk.gov.ida.hub.config.validators;

import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

public class TransactionConfigMatchingServiceValidator {
    public void validate(TransactionConfigEntityData transactionConfig, ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository) {
        if (!matchingServiceConfigEntityDataRepository.getData(transactionConfig.getMatchingServiceEntityId()).isPresent()) {
            throw ConfigValidationException.createAbsentMatchingServiceConfigException(transactionConfig.getMatchingServiceEntityId(), transactionConfig.getEntityId());
        }
    }
}
