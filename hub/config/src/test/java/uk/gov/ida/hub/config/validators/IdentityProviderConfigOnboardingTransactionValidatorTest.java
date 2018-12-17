package uk.gov.ida.hub.config.validators;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createAbsentOnboardingTransactionConfigException;

@RunWith(MockitoJUnitRunner.class)
public class IdentityProviderConfigOnboardingTransactionValidatorTest {

    IdentityProviderConfigOnboardingTransactionValidator identityProviderConfigOnboardingTransactionValidator;

    @Mock
    private ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataConfigEntityDataRepository;

    @Before
    public void setUp() throws Exception {
        identityProviderConfigOnboardingTransactionValidator = new IdentityProviderConfigOnboardingTransactionValidator(
                transactionConfigEntityDataConfigEntityDataRepository
        );
    }

    @Test
    public void validate_shouldNotThrowExceptionWhenOnboardingTransactionEntityIdExists() throws Exception {
        String transactionEntityID = "transactionEntityID";
        IdentityProviderConfigEntityData identityProviderConfigEntityData = anIdentityProviderConfigData().withOnboarding(ImmutableList.of(transactionEntityID)).build();

        TransactionConfigEntityData transactionConfigEntity = aTransactionConfigData().build();
        when(transactionConfigEntityDataConfigEntityDataRepository.getData(transactionEntityID)).thenReturn(Optional.ofNullable(transactionConfigEntity));

        identityProviderConfigOnboardingTransactionValidator.validate(identityProviderConfigEntityData);
    }

    @Test
    public void validate_shouldNotThrowExceptionWhenOnboardingTransactionEntityIsNotSpecified() throws Exception {
        IdentityProviderConfigEntityData identityProviderConfigEntityData = anIdentityProviderConfigData().withoutOnboarding().build();
        identityProviderConfigOnboardingTransactionValidator.validate(identityProviderConfigEntityData);
    }

    @Test
    public void validate_shouldThrowExceptionWhenOnboardingTransactionDoesNotExist() throws Exception {
        String transactionEntityID = "transactionEntityID";
        String idpEntityId = "idpEntityId";
        IdentityProviderConfigEntityData identityProviderConfigEntityData = anIdentityProviderConfigData()
                .withEntityId(idpEntityId)
                .withOnboarding(ImmutableList.of(transactionEntityID))
                .build();

        when(transactionConfigEntityDataConfigEntityDataRepository.getData(transactionEntityID))
                .thenReturn(Optional.empty());

        try {
            identityProviderConfigOnboardingTransactionValidator.validate(identityProviderConfigEntityData);
            fail("fail");
        } catch (ConfigValidationException e) {
            final ConfigValidationException expectedException = createAbsentOnboardingTransactionConfigException(transactionEntityID, idpEntityId);

            assertThat(expectedException.getMessage()).isEqualTo(e.getMessage());
        }
    }
}
