package uk.gov.ida.hub.config.validators;

import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

public class TransactionConfigMatchingServiceValidator {
    public void validate(TransactionConfig transactionConfig, LocalConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository) {
        if (transactionConfig.isUsingMatching()) {
            if (transactionConfig.getMatchingServiceEntityId() == null) {
                throw ConfigValidationException.createMissingMatchingEntityIdException(transactionConfig.getEntityId());
            }
            if (!matchingServiceConfigRepository.getData(transactionConfig.getMatchingServiceEntityId()).isPresent()) {
                throw ConfigValidationException.createAbsentMatchingServiceConfigException(transactionConfig.getMatchingServiceEntityId(), transactionConfig.getEntityId());
            }
        }
    }
}
