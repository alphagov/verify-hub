package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableSet;
import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.hub.config.domain.EntityIdentifiable;
import uk.gov.ida.hub.config.annotations.CertificateConfigValidator;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CountryConfig;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.TranslationData;
import uk.gov.ida.hub.config.validators.DuplicateEntityIdConfigValidator;
import uk.gov.ida.hub.config.validators.IdentityProviderConfigOnboardingTransactionValidator;
import uk.gov.ida.hub.config.validators.TransactionConfigMatchingServiceValidator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigDataBootstrap implements Managed {

    private final ConfigDataSource<IdentityProviderConfig> identityProviderConfigDataSource;
    private final ConfigDataSource<MatchingServiceConfig> matchingServiceConfigDataSource;
    private final ConfigDataSource<TransactionConfig> transactionConfigDataSource;
    private final ConfigDataSource<TranslationData> translationsDataSource;
    private final ConfigDataSource<CountryConfig> countriesConfigDataSource;

    private final LocalConfigRepository<IdentityProviderConfig> identityProviderConfigRepository;
    private LocalConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository = null;
    private LocalConfigRepository<TransactionConfig> transactionConfigRepository = null;
    private final LocalConfigRepository<TranslationData> translationsRepository;
    private final LocalConfigRepository<CountryConfig> countryConfigRepository;
    private CertificateChainConfigValidator certificateChainConfigValidator = null;
    private LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator = null;

    private enum EntityData {IDENTITY_PROVIDER, MATCHING_SERVICE, TRANSACTION, COUNTRIES, TRANSLATIONS}

    @Inject
    public ConfigDataBootstrap(
            ConfigDataSource<IdentityProviderConfig> identityProviderConfigDataSource,
            ConfigDataSource<MatchingServiceConfig> matchingServiceConfigDataSource,
            ConfigDataSource<TransactionConfig> transactionConfigDataSource,
            ConfigDataSource<TranslationData> translationsDataSource,
            ConfigDataSource<CountryConfig> countriesConfigDataSource,
            LocalConfigRepository<IdentityProviderConfig> identityProviderConfigRepository,
            LocalConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository,
            LocalConfigRepository<TransactionConfig> transactionConfigRepository,
            LocalConfigRepository<TranslationData> translationsRepository,
            LocalConfigRepository<CountryConfig> countryConfigRepository,
            @CertificateConfigValidator CertificateChainConfigValidator certificateChainConfigValidator,
            LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator) {

        this.identityProviderConfigDataSource = identityProviderConfigDataSource;
        this.matchingServiceConfigDataSource = matchingServiceConfigDataSource;
        this.transactionConfigDataSource = transactionConfigDataSource;
        this.translationsDataSource = translationsDataSource;
        this.countriesConfigDataSource = countriesConfigDataSource;
        this.identityProviderConfigRepository = identityProviderConfigRepository;
        this.matchingServiceConfigRepository = matchingServiceConfigRepository;
        this.transactionConfigRepository = transactionConfigRepository;
        this.translationsRepository = translationsRepository;
        this.countryConfigRepository = countryConfigRepository;
        this.certificateChainConfigValidator = certificateChainConfigValidator;
        this.levelsOfAssuranceConfigValidator = levelsOfAssuranceConfigValidator;
    }

    @Override
    public void start() {
        Map<EntityData, Collection<? extends EntityIdentifiable>> allConfig = new HashMap<>();
        allConfig.put(EntityData.IDENTITY_PROVIDER, identityProviderConfigDataSource.loadConfig());
        allConfig.put(EntityData.MATCHING_SERVICE, matchingServiceConfigDataSource.loadConfig());
        allConfig.put(EntityData.TRANSACTION, transactionConfigDataSource.loadConfig());
        allConfig.put(EntityData.COUNTRIES, countriesConfigDataSource.loadConfig());
        allConfig.put(EntityData.TRANSLATIONS, translationsDataSource.loadConfig());

        cacheConfigData(allConfig);

        checkForDuplicateEntityIds
            .andThen(checkEachTransactionHasCorrespondingEntryInMatchingService)
            .andThen(checkEachOnboardingIdentityProviderHasACorrespondingTransaction)
            .andThen(checkThereAreNoInvalidCertificates)
            .andThen(checkIdpAndTransactionsHaveValidLevelsOfAssurance)
            .accept(allConfig);
    }

    @Override
    public void stop() {
        // don't need to do anything
    }

    private void cacheConfigData(Map<EntityData, Collection<? extends EntityIdentifiable>> allConfig) {
        identityProviderConfigRepository.addData((Collection<IdentityProviderConfig>)allConfig.get(EntityData.IDENTITY_PROVIDER));
        matchingServiceConfigRepository.addData((Collection<MatchingServiceConfig>)allConfig.get(EntityData.MATCHING_SERVICE));
        transactionConfigRepository.addData((Collection<TransactionConfig>)allConfig.get(EntityData.TRANSACTION));
        countryConfigRepository.addData((Collection<CountryConfig>)allConfig.get(EntityData.COUNTRIES));
        translationsRepository.addData((Collection<TranslationData>)allConfig.get(EntityData.TRANSLATIONS));
    }

    private Consumer<Map<EntityData, Collection<? extends EntityIdentifiable>>> checkForDuplicateEntityIds = dataSource -> {
        Collection<EntityIdentifiable> allConfigEntityData = new ArrayList<>();
        allConfigEntityData.addAll(dataSource.get(EntityData.IDENTITY_PROVIDER));
        allConfigEntityData.addAll(dataSource.get(EntityData.MATCHING_SERVICE));
        allConfigEntityData.addAll(dataSource.get(EntityData.TRANSACTION));
        allConfigEntityData.addAll(dataSource.get(EntityData.TRANSLATIONS));
        allConfigEntityData.addAll(dataSource.get(EntityData.COUNTRIES));

        DuplicateEntityIdConfigValidator duplicateEntityIdConfigValidator = new DuplicateEntityIdConfigValidator();
        duplicateEntityIdConfigValidator.validate(allConfigEntityData);
    };

    private Consumer<Map<EntityData, Collection<? extends EntityIdentifiable>>> checkEachTransactionHasCorrespondingEntryInMatchingService = dataSource -> {
        TransactionConfigMatchingServiceValidator transactionConfigMatchingServiceValidator = new TransactionConfigMatchingServiceValidator();
        dataSource.get(EntityData.TRANSACTION)
                .forEach(data -> transactionConfigMatchingServiceValidator
                        .validate((TransactionConfig)data, matchingServiceConfigRepository));
    };

    private Consumer<Map<EntityData, Collection<? extends EntityIdentifiable>>> checkEachOnboardingIdentityProviderHasACorrespondingTransaction = dataSource -> {
        IdentityProviderConfigOnboardingTransactionValidator identityProviderConfigOnboardingTransactionValidator = new IdentityProviderConfigOnboardingTransactionValidator(transactionConfigRepository);
        dataSource.get(EntityData.IDENTITY_PROVIDER)
                .forEach(data -> identityProviderConfigOnboardingTransactionValidator.validate((IdentityProviderConfig)data));
    };

    private Consumer<Map<EntityData, Collection<? extends EntityIdentifiable>>> checkThereAreNoInvalidCertificates = dataSource -> {
        final ImmutableSet<TransactionConfig> transactionConfigs = ImmutableSet.copyOf((Collection<TransactionConfig>)dataSource.get(EntityData.TRANSACTION));

        certificateChainConfigValidator.validate(transactionConfigs, ImmutableSet.copyOf((Collection<MatchingServiceConfig>)dataSource.get(EntityData.MATCHING_SERVICE)));
    };

    private Consumer<Map<EntityData, Collection<? extends EntityIdentifiable>>> checkIdpAndTransactionsHaveValidLevelsOfAssurance = dataSource -> {
        final Collection<IdentityProviderConfig> enabledIdentityProviders = (Collection<IdentityProviderConfig>)dataSource.get(EntityData.IDENTITY_PROVIDER);
        enabledIdentityProviders
                .stream()
                .filter(input -> input.isEnabled())
                .collect(Collectors.toList());

        final ImmutableSet<IdentityProviderConfig> identityProviderConfigs = ImmutableSet.copyOf(enabledIdentityProviders);
        final ImmutableSet<TransactionConfig> transactionConfigs = ImmutableSet.copyOf((Collection<TransactionConfig>)dataSource.get(EntityData.TRANSACTION));

        levelsOfAssuranceConfigValidator.validateLevelsOfAssurance(identityProviderConfigs, transactionConfigs);
    };
}
