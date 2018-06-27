package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.config.ConfigEntityData;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CertificateType;
import uk.gov.ida.hub.config.domain.CountriesConfigEntityData;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.domain.TranslationData;
import uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.security.cert.CertPathValidatorException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.domain.builders.TranslationDataBuilder.aTranslationData;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createAbsentOnboardingTransactionConfigException;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createInvalidCertificatesException;

@RunWith(MockitoJUnitRunner.class)
public class ConfigDataBootstrapTest {
    private static final String MATCHING_SERVICE_ENTITY_ID = "matching-service-entity-id";
    private static final String NON_EXISTENT_MATCHING_SERVICE_ENTITY_ID = "non-existent-matching-service-entity-id";

    @Mock
    private CertificateChainConfigValidator certificateChainConfigValidator;

    private final ConfigEntityDataRepository<? extends ConfigEntityData> nullConfigEntityDataRepository = new ConfigEntityDataRepository<>();
    private final LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator = new LevelsOfAssuranceConfigValidator();

    @Test
    public void start_shouldThrowExceptionWhenDuplicateEntityIdsExist() throws Exception {
        final String entityId = "entity-id";
        final String simpleId = "simple-id";
        final String matchingServiceEntityId = "matching-service-entity-id";
        final IdentityProviderConfigEntityData identityProviderConfigData = anIdentityProviderConfigData().withEntityId(entityId).build();
        final TransactionConfigEntityData transactionConfigData = aTransactionConfigData()
                .withEntityId(entityId)
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();
        final MatchingServiceConfigEntityData matchingServiceConfigData = aMatchingServiceConfigEntityData()
                .withEntityId(matchingServiceEntityId)
                .build();

        final CountriesConfigEntityData countriesConfigEntityData = new CountriesConfigEntityData() {};

        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                identityProviderConfigData, 
                matchingServiceConfigData, 
                transactionConfigData, 
                translationData,
                countriesConfigEntityData
        );

        try {
            configDataBootstrap.start();
            fail("fail");
        } catch (ConfigValidationException e) {
            assertThat(e.getMessage()).isEqualTo(ConfigValidationException.createDuplicateEntityIdException(entityId).getMessage());
        }
    }

    @Test
    public void start_shouldThrowExceptionWhenOnboardingTransactionEntityIdCheckFails() throws Exception {
        final String idpEntityId = "idp-entity-id";
        final String simpleId = "simple-id";
        final String matchingServiceEntityId = "matching-service-entity-id";
        final String nonExistentTransactionEntityId = "non-existent-transaction";
        final IdentityProviderConfigEntityData identityProviderConfigData = anIdentityProviderConfigData().withEntityId(idpEntityId).withOnboarding(ImmutableList.of(nonExistentTransactionEntityId)).build();
        final TransactionConfigEntityData transactionConfigData = aTransactionConfigData().withEntityId("transaction-entity-id").withMatchingServiceEntityId(matchingServiceEntityId).build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();
        final CountriesConfigEntityData countriesConfigEntityData = new CountriesConfigEntityData() {};

        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                identityProviderConfigData, 
                aMatchingServiceConfigEntityData().withEntityId(matchingServiceEntityId).build(), 
                transactionConfigData, 
                translationData,
                countriesConfigEntityData
        );

        try {
            configDataBootstrap.start();
            fail("Onboarding transaction entity id check did not fail.");
        } catch (ConfigValidationException e) {
            assertThat(e.getMessage()).isEqualTo(createAbsentOnboardingTransactionConfigException(nonExistentTransactionEntityId, idpEntityId).getMessage());
        }
    }

    @Test
    public void start_shouldThrowExceptionWhenMatchingTransactionEntityIdCheckFails() throws Exception {
        final String transEntityId = "trans-entity-id";
        final String simpleId = "simple-id";
        final IdentityProviderConfigEntityData identityProviderConfigData = anIdentityProviderConfigData().withEntityId("entity-id").build();
        final TransactionConfigEntityData transactionConfigData = aTransactionConfigData()
                .withEntityId(transEntityId)
                .withMatchingServiceEntityId(NON_EXISTENT_MATCHING_SERVICE_ENTITY_ID)
                .build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();
        final MatchingServiceConfigEntityData matchingServiceConfigData = aMatchingServiceConfigEntityData()
                .withEntityId(MATCHING_SERVICE_ENTITY_ID)
                .build();

        final CountriesConfigEntityData countriesConfigEntityData = new CountriesConfigEntityData() {};

        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                identityProviderConfigData, 
                matchingServiceConfigData, 
                transactionConfigData, 
                translationData,
                countriesConfigEntityData
        );

        try {
            configDataBootstrap.start();
            fail("fail");
        } catch (ConfigValidationException e) {
            assertThat(e.getMessage()).isEqualTo(ConfigValidationException.createAbsentMatchingServiceConfigException(NON_EXISTENT_MATCHING_SERVICE_ENTITY_ID, transEntityId).getMessage());
        }
    }


    @Ignore
    public void continuesToStart_WhenCertificateCheckHasInvalidCertificates() {
        final String idpEntityId = "idp-entity-id";
        final String simpleId = "simple-id";
        final String matchingServiceId = "matching-service-id";
        final String rpEntityId = "rp-entity";
        String badCertificateValue = "badCertificate";
        final SignatureVerificationCertificate badCertificate = new SignatureVerificationCertificateBuilder().withX509(badCertificateValue).build();
        final IdentityProviderConfigEntityData identityProviderConfigData = anIdentityProviderConfigData().withEntityId(idpEntityId).addSignatureVerificationCertificate(badCertificate).build();


        MatchingServiceConfigEntityData matchingServiceConfigData = aMatchingServiceConfigEntityData().addSignatureVerificationCertificate(badCertificate).withEntityId(matchingServiceId).build();
        TransactionConfigEntityData transactionConfigData = aTransactionConfigData().withMatchingServiceEntityId(matchingServiceId).withEntityId(rpEntityId).build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();

        InvalidCertificateDto invalidIdpCertificateDto = new InvalidCertificateDto(idpEntityId, CertPathValidatorException.BasicReason.INVALID_SIGNATURE, CertificateType.SIGNING, FederationEntityType.IDP, "certificate was bad!");
        InvalidCertificateDto invalidMatchingServiceCertificateDto = new InvalidCertificateDto(matchingServiceId, CertPathValidatorException.BasicReason.NOT_YET_VALID, CertificateType.SIGNING, FederationEntityType.MS, "certificate was not yet valid!");

        doThrow(createInvalidCertificatesException(ImmutableList.of(invalidMatchingServiceCertificateDto, invalidIdpCertificateDto))).when(certificateChainConfigValidator).validate(
                ImmutableSet.of(transactionConfigData),
                ImmutableSet.of(matchingServiceConfigData));

        CountriesConfigEntityData countriesConfigEntityData = createCountriesConfigEntityData();
        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                identityProviderConfigData, 
                matchingServiceConfigData, 
                transactionConfigData, 
                translationData,
                countriesConfigEntityData
        );
        configDataBootstrap.start();
    }

    @Test
    public void start_shouldOnlyValidateCertificateChainIfIdentityProviderIsEnabled() throws Exception {
        final String simpleId = "simple-id";
        IdentityProviderConfigEntityData disabledIdp = anIdentityProviderConfigData().withEntityId("idp1EntityId").withEnabled(false).build();
        MatchingServiceConfigEntityData matchingServiceConfigData = aMatchingServiceConfigEntityData().withEntityId("matchingServiceId").build();
        TransactionConfigEntityData transactionConfigData = aTransactionConfigData()
                .withMatchingServiceEntityId("matchingServiceId")
                .withEntityId("rpEntityId")
                .build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();
        CountriesConfigEntityData countriesConfigData = new CountriesConfigEntityData() {};

        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                disabledIdp, 
                matchingServiceConfigData, 
                transactionConfigData, 
                translationData,
                countriesConfigData
        );
        configDataBootstrap.start();

        verify(certificateChainConfigValidator).validate(ImmutableSet.of(transactionConfigData), ImmutableSet.of(matchingServiceConfigData));
    }

    private CountriesConfigEntityData createCountriesConfigEntityData() {
        return new CountriesConfigEntityData() {};
    }

    private ConfigDataBootstrap createConfigDataBootstrap(IdentityProviderConfigEntityData identityProviderConfigData,
                                                          MatchingServiceConfigEntityData matchingServiceConfigData,
                                                          TransactionConfigEntityData transactionConfigData,
                                                          TranslationData translationData,
                                                          CountriesConfigEntityData countriesConfigData) {
        return new ConfigDataBootstrap(
                new TestConfigDataSource<>(identityProviderConfigData),
                new TestConfigDataSource<>(matchingServiceConfigData),
                new TestConfigDataSource<>(transactionConfigData),
                new TestConfigDataSource<>(translationData),
                new TestConfigDataSource<>(countriesConfigData),
                (ConfigEntityDataRepository<IdentityProviderConfigEntityData>) nullConfigEntityDataRepository,
                (ConfigEntityDataRepository<MatchingServiceConfigEntityData>) nullConfigEntityDataRepository,
                (ConfigEntityDataRepository<TransactionConfigEntityData>) nullConfigEntityDataRepository, 
                (ConfigEntityDataRepository<TranslationData>) nullConfigEntityDataRepository,
                (ConfigEntityDataRepository<CountriesConfigEntityData>) nullConfigEntityDataRepository,
                certificateChainConfigValidator,
                levelsOfAssuranceConfigValidator);
    }

    private class TestConfigDataSource<T> implements ConfigDataSource<T> {

        private final Collection<T> configData = new ArrayList<>();

        public TestConfigDataSource(T... configDataItems) {
            Collections.addAll(configData, configDataItems);
        }

        @Override
        public Collection<T> loadConfig() {
            return configData;
        }
    }

}
