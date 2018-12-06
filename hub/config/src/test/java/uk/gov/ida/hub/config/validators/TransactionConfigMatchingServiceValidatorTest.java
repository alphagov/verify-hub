package uk.gov.ida.hub.config.validators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createAbsentMatchingServiceConfigException;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createMissingMatchingEntityIdException;

@RunWith(MockitoJUnitRunner.class)
public class TransactionConfigMatchingServiceValidatorTest {

    private TransactionConfigMatchingServiceValidator validator = new TransactionConfigMatchingServiceValidator();

    @Mock
    private ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository;

    @Test
    public void matchingServiceEntityId_shouldHaveCorrespondingConfigurationWhenUsingMatching() throws Exception {
        final String matchingServiceEntityId = "matching-service-entity-id";
        TransactionConfigEntityData transactionConfig = aTransactionConfigData()
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .build();
        MatchingServiceConfigEntityData matchingServiceConfigData = aMatchingServiceConfigEntityData()
                .withEntityId(matchingServiceEntityId)
                .build();

        when(matchingServiceConfigEntityDataRepository.getData(matchingServiceEntityId)).thenReturn(Optional.ofNullable(matchingServiceConfigData));

        validator.validate(transactionConfig, matchingServiceConfigEntityDataRepository);
    }

    @Test
    public void matchingServiceEntityId_doesNotNeedCorrespondingConfigurationWhenNotUsingMatching() throws Exception {
        final String matchingServiceEntityId = "matching-service-entity-id";
        TransactionConfigEntityData transactionConfig = aTransactionConfigData()
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .withUsingMatching(false)
                .build();

        validator.validate(transactionConfig, matchingServiceConfigEntityDataRepository);
    }

    @Test
    public void validator_shouldThrowExceptionWhenMatchingEntityIdIsAbsentIfUsingMatching() throws Exception {
        TransactionConfigEntityData transactionConfig = aTransactionConfigData()
                .withMatchingServiceEntityId(null)
                .build();

        try {
            validator.validate(transactionConfig, matchingServiceConfigEntityDataRepository);
            fail("fail");
        } catch(ConfigValidationException configValidationException) {
            final ConfigValidationException expectedException = createMissingMatchingEntityIdException(transactionConfig.getEntityId());
            assertThat(configValidationException.getMessage()).isEqualTo(expectedException.getMessage());
        }
    }

    @Test
    public void validator_shouldThrowExceptionWhenCorrespondingMatchingServiceConfigurationIsAbsent() throws Exception {
        final String matchingServiceEntityId = "matching-service-entity-id";
        TransactionConfigEntityData transactionConfig = aTransactionConfigData()
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .build();
        when(matchingServiceConfigEntityDataRepository.getData(matchingServiceEntityId)).thenReturn(Optional.empty());

        try {
            validator.validate(transactionConfig, matchingServiceConfigEntityDataRepository);
            fail("fail");
        } catch(ConfigValidationException configValidationException) {
            final ConfigValidationException expectedException = createAbsentMatchingServiceConfigException(matchingServiceEntityId, transactionConfig.getEntityId());
            assertThat(configValidationException.getMessage()).isEqualTo(expectedException.getMessage());
        }
    }
}
