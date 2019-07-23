package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CertificateType;
import uk.gov.ida.hub.config.domain.CountryConfig;
import uk.gov.ida.hub.config.domain.EntityIdentifiable;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.TranslationData;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.security.cert.CertPathValidatorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.hub.config.domain.builders.TranslationDataBuilder.aTranslationData;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createAbsentOnboardingTransactionConfigException;
import static uk.gov.ida.hub.config.exceptions.ConfigValidationException.createInvalidCertificatesException;

@RunWith(MockitoJUnitRunner.class)
public class ConfigDataBootstrapTest {
    private static final String MATCHING_SERVICE_ENTITY_ID = "matching-service-entity-id";
    private static final String NON_EXISTENT_MATCHING_SERVICE_ENTITY_ID = "non-existent-matching-service-entity-id";

    @Mock
    private CertificateChainConfigValidator certificateChainConfigValidator;

    private final LocalConfigRepository<? extends EntityIdentifiable> nullConfigRepository = new LocalConfigRepository<>();
    private final LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator = new LevelsOfAssuranceConfigValidator();

    @Test
    public void start_shouldThrowExceptionWhenDuplicateEntityIdsExist() throws Exception {
        final String entityId = "entity-id";
        final String simpleId = "simple-id";
        final String matchingServiceEntityId = "matching-service-entity-id";
        final IdentityProviderConfig identityProviderConfigData = anIdentityProviderConfigData().withEntityId(entityId).build();
        final TransactionConfig transactionConfigData = aTransactionConfigData()
                .withEntityId(entityId)
                .withMatchingServiceEntityId(matchingServiceEntityId)
                .build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();
        final MatchingServiceConfig matchingServiceConfigData = aMatchingServiceConfig()
                .withEntityId(matchingServiceEntityId)
                .build();

        final CountryConfig countryConfig = new CountryConfig() {};

        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                identityProviderConfigData, 
                matchingServiceConfigData, 
                transactionConfigData, 
                translationData,
                countryConfig
        );

        try {
            configDataBootstrap.start();
            fail("fail");
        } catch (ConfigValidationException e) {
            assertThat(e.getMessage()).isEqualTo(ConfigValidationException.createDuplicateEntityIdException(entityId).getMessage());
        }
    }

    @Test
    public void start_shouldThrowExceptionWhenOnboardingTransactionEntityIdCheckFails() {
        final String idpEntityId = "idp-entity-id";
        final String simpleId = "simple-id";
        final String matchingServiceEntityId = "matching-service-entity-id";
        final String nonExistentTransactionEntityId = "non-existent-transaction";
        final IdentityProviderConfig identityProviderConfigData = anIdentityProviderConfigData().withEntityId(idpEntityId).withOnboarding(ImmutableList.of(nonExistentTransactionEntityId)).build();
        final TransactionConfig transactionConfigData = aTransactionConfigData().withEntityId("transaction-entity-id").withMatchingServiceEntityId(matchingServiceEntityId).build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();
        final CountryConfig countryConfig = new CountryConfig() {};

        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                identityProviderConfigData, 
                aMatchingServiceConfig().withEntityId(matchingServiceEntityId).build(),
                transactionConfigData, 
                translationData,
                countryConfig
        );

        try {
            configDataBootstrap.start();
            fail("Onboarding transaction entity id check did not fail.");
        } catch (ConfigValidationException e) {
            assertThat(e.getMessage()).isEqualTo(createAbsentOnboardingTransactionConfigException(nonExistentTransactionEntityId, idpEntityId).getMessage());
        }
    }

    @Test
    public void start_shouldThrowExceptionWhenMatchingTransactionEntityIdCheckFails() {
        final String transEntityId = "trans-entity-id";
        final String simpleId = "simple-id";
        final IdentityProviderConfig identityProviderConfigData = anIdentityProviderConfigData().withEntityId("entity-id").build();
        final TransactionConfig transactionConfigData = aTransactionConfigData()
                .withEntityId(transEntityId)
                .withMatchingServiceEntityId(NON_EXISTENT_MATCHING_SERVICE_ENTITY_ID)
                .build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();
        final MatchingServiceConfig matchingServiceConfigData = aMatchingServiceConfig()
                .withEntityId(MATCHING_SERVICE_ENTITY_ID)
                .build();

        final CountryConfig countryConfig = new CountryConfig() {};

        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                identityProviderConfigData, 
                matchingServiceConfigData, 
                transactionConfigData, 
                translationData,
                countryConfig
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
        final IdentityProviderConfig identityProviderConfigData = anIdentityProviderConfigData().withEntityId(idpEntityId).build();

        MatchingServiceConfig matchingServiceConfigData = aMatchingServiceConfig().addSignatureVerificationCertificate(badCertificateValue).withEntityId(matchingServiceId).build();
        TransactionConfig transactionConfigData = aTransactionConfigData().withMatchingServiceEntityId(matchingServiceId).withEntityId(rpEntityId).build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();

        InvalidCertificateDto invalidIdpCertificateDto = new InvalidCertificateDto(idpEntityId, CertPathValidatorException.BasicReason.INVALID_SIGNATURE, CertificateType.SIGNING, FederationEntityType.IDP, "certificate was bad!");
        InvalidCertificateDto invalidMatchingServiceCertificateDto = new InvalidCertificateDto(matchingServiceId, CertPathValidatorException.BasicReason.NOT_YET_VALID, CertificateType.SIGNING, FederationEntityType.MS, "certificate was not yet valid!");

        doThrow(createInvalidCertificatesException(ImmutableList.of(invalidMatchingServiceCertificateDto, invalidIdpCertificateDto))).when(certificateChainConfigValidator).validate(
                ImmutableSet.of(transactionConfigData),
                ImmutableSet.of(matchingServiceConfigData));

        CountryConfig countryConfig = createCountriesConfig();
        ConfigDataBootstrap configDataBootstrap = createConfigDataBootstrap(
                identityProviderConfigData, 
                matchingServiceConfigData, 
                transactionConfigData, 
                translationData,
                countryConfig
        );
        configDataBootstrap.start();
    }

    @Test
    public void start_shouldOnlyValidateCertificateChainIfIdentityProviderIsEnabled() {
        final String simpleId = "simple-id";
        IdentityProviderConfig disabledIdp = anIdentityProviderConfigData().withEntityId("idp1EntityId").withEnabled(false).build();
        MatchingServiceConfig matchingServiceConfigData = aMatchingServiceConfig().withEntityId("matchingServiceId").build();
        TransactionConfig transactionConfigData = aTransactionConfigData()
                .withMatchingServiceEntityId("matchingServiceId")
                .withEntityId("rpEntityId")
                .build();
        final TranslationData translationData = aTranslationData().withSimpleId(simpleId).build();
        CountryConfig countriesConfigData = new CountryConfig() {};

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

    private CountryConfig createCountriesConfig() {
        return new CountryConfig() {};
    }

    private ConfigDataBootstrap createConfigDataBootstrap(IdentityProviderConfig identityProviderConfigData,
                                                          MatchingServiceConfig matchingServiceConfigData,
                                                          TransactionConfig transactionConfigData,
                                                          TranslationData translationData,
                                                          CountryConfig countriesConfigData) {
        return new ConfigDataBootstrap(
                new TestConfigDataSource<>(identityProviderConfigData),
                new TestConfigDataSource<>(matchingServiceConfigData),
                new TestConfigDataSource<>(transactionConfigData),
                new TestConfigDataSource<>(translationData),
                new TestConfigDataSource<>(countriesConfigData),
                (LocalConfigRepository<IdentityProviderConfig>) nullConfigRepository,
                (LocalConfigRepository<MatchingServiceConfig>) nullConfigRepository,
                (LocalConfigRepository<TransactionConfig>) nullConfigRepository,
                (LocalConfigRepository<TranslationData>) nullConfigRepository,
                (LocalConfigRepository<CountryConfig>) nullConfigRepository,
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
