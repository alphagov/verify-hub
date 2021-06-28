package uk.gov.ida.hub.config.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createAbsentMatchingServiceConfigException;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createMissingMatchingEntityIdException;

@ExtendWith(MockitoExtension.class)
public class TransactionConfigMatchingServiceValidatorTest {

    @Mock
    private LocalConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository;

    private TransactionConfigMatchingServiceValidator validator;

    @BeforeEach
    public void setUp(){
        validator = new TransactionConfigMatchingServiceValidator(matchingServiceConfigRepository);
    }

    @Test
    public void matchingServiceEntityId_shouldHaveCorrespondingConfigurationWhenUsingMatching() {
        final String matchingServiceEntityId = "matching-service-entity-id";
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .build();
        MatchingServiceConfig matchingServiceConfigData = aMatchingServiceConfig()
                .withEntityId(matchingServiceEntityId)
                .build();

        when(matchingServiceConfigRepository.getData(matchingServiceEntityId)).thenReturn(Optional.ofNullable(matchingServiceConfigData));

        validator.validate(transactionConfig);
    }

    @Test
    public void matchingServiceEntityId_doesNotNeedCorrespondingConfigurationWhenNotUsingMatching() {
        final String matchingServiceEntityId = "matching-service-entity-id";
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .withUsingMatching(false)
                .build();

        validator.validate(transactionConfig);
    }

    @Test
    public void validator_shouldThrowExceptionWhenMatchingEntityIdIsAbsentIfUsingMatching() {
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withMatchingServiceEntityId(null)
                .build();

        try {
            validator.validate(transactionConfig);
            fail("fail");
        } catch(ConfigValidationException configValidationException) {
            final ConfigValidationException expectedException = createMissingMatchingEntityIdException(transactionConfig.getEntityId());
            assertThat(configValidationException.getMessage()).isEqualTo(expectedException.getMessage());
        }
    }

    @Test
    public void validator_shouldThrowExceptionWhenCorrespondingMatchingServiceConfigurationIsAbsent() {
        final String matchingServiceEntityId = "matching-service-entity-id";
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .build();
        when(matchingServiceConfigRepository.getData(matchingServiceEntityId)).thenReturn(Optional.empty());

        try {
            validator.validate(transactionConfig);
            fail("fail");
        } catch(ConfigValidationException configValidationException) {
            final ConfigValidationException expectedException = createAbsentMatchingServiceConfigException(matchingServiceEntityId, transactionConfig.getEntityId());
            assertThat(configValidationException.getMessage()).isEqualTo(expectedException.getMessage());
        }
    }
}
