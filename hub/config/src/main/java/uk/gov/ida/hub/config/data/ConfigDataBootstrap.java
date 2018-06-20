package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableSet;
import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.hub.config.ConfigEntityData;
import uk.gov.ida.hub.config.annotations.CertificateConfigValidator;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CountriesConfigEntityData;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
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

    private final ConfigDataSource<IdentityProviderConfigEntityData> identityProviderConfigDataSource;
    private final ConfigDataSource<MatchingServiceConfigEntityData> matchingServiceConfigDataSource;
    private final ConfigDataSource<TransactionConfigEntityData> transactionConfigDataSource;
    private final ConfigDataSource<TranslationData> translationsDataSource;
    private final ConfigDataSource<CountriesConfigEntityData> countriesConfigDataSource;

    private final ConfigEntityDataRepository<IdentityProviderConfigEntityData> identityProviderConfigEntityDataRepository;
    private ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository = null;
    private ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository = null;
    private final ConfigEntityDataRepository<TranslationData> translationsRepository;
    private final ConfigEntityDataRepository<CountriesConfigEntityData> countriesConfigEntityDataConfigEntityDataRepository;
    private CertificateChainConfigValidator certificateChainConfigValidator = null;
    private LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator = null;

    private enum EntityData {IDENTITY_PROVIDER, MATCHING_SERVICE, TRANSACTION, COUNTRIES, TRANSLATIONS}

    @Inject
    public ConfigDataBootstrap(
            ConfigDataSource<IdentityProviderConfigEntityData> identityProviderConfigDataSource,
            ConfigDataSource<MatchingServiceConfigEntityData> matchingServiceConfigDataSource,
            ConfigDataSource<TransactionConfigEntityData> transactionConfigDataSource,
            ConfigDataSource<TranslationData> translationsDataSource,
            ConfigDataSource<CountriesConfigEntityData> countriesConfigDataSource,
            ConfigEntityDataRepository<IdentityProviderConfigEntityData> identityProviderConfigEntityDataRepository,
            ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository,
            ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository,
            ConfigEntityDataRepository<TranslationData> translationsRepository,
            ConfigEntityDataRepository<CountriesConfigEntityData> countriesConfigEntityDataConfigEntityDataRepository,
            @CertificateConfigValidator CertificateChainConfigValidator certificateChainConfigValidator,
            LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator) {

        this.identityProviderConfigDataSource = identityProviderConfigDataSource;
        this.matchingServiceConfigDataSource = matchingServiceConfigDataSource;
        this.transactionConfigDataSource = transactionConfigDataSource;
        this.translationsDataSource = translationsDataSource;
        this.countriesConfigDataSource = countriesConfigDataSource;
        this.identityProviderConfigEntityDataRepository = identityProviderConfigEntityDataRepository;
        this.matchingServiceConfigEntityDataRepository = matchingServiceConfigEntityDataRepository;
        this.transactionConfigEntityDataRepository = transactionConfigEntityDataRepository;
        this.translationsRepository = translationsRepository;
        this.countriesConfigEntityDataConfigEntityDataRepository = countriesConfigEntityDataConfigEntityDataRepository;
        this.certificateChainConfigValidator = certificateChainConfigValidator;
        this.levelsOfAssuranceConfigValidator = levelsOfAssuranceConfigValidator;
    }

    @Override
    public void start() {
        Map<EntityData, Collection<? extends ConfigEntityData>> allConfig = new HashMap<>();
        allConfig.put(EntityData.IDENTITY_PROVIDER, identityProviderConfigDataSource.loadConfig());
        allConfig.put(EntityData.MATCHING_SERVICE, matchingServiceConfigDataSource.loadConfig());
        allConfig.put(EntityData.TRANSACTION, transactionConfigDataSource.loadConfig());
        allConfig.put(EntityData.COUNTRIES, countriesConfigDataSource.loadConfig());
        allConfig.put(EntityData.TRANSLATIONS, translationsDataSource.loadConfig());

        cacheConfigData(allConfig);

        // TODO: Implement validation for translations as well.
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

    private void cacheConfigData(Map<EntityData, Collection<? extends ConfigEntityData>> allConfig) {
        identityProviderConfigEntityDataRepository.addData((Collection<IdentityProviderConfigEntityData>)allConfig.get(EntityData.IDENTITY_PROVIDER));
        matchingServiceConfigEntityDataRepository.addData((Collection<MatchingServiceConfigEntityData>)allConfig.get(EntityData.MATCHING_SERVICE));
        transactionConfigEntityDataRepository.addData((Collection<TransactionConfigEntityData>)allConfig.get(EntityData.TRANSACTION));
        countriesConfigEntityDataConfigEntityDataRepository.addData((Collection<CountriesConfigEntityData>)allConfig.get(EntityData.COUNTRIES));
        translationsRepository.addData((Collection<TranslationData>)allConfig.get(EntityData.TRANSLATIONS));
    }

    private Consumer<Map<EntityData, Collection<? extends ConfigEntityData>>> checkForDuplicateEntityIds = dataSource -> {
        Collection<ConfigEntityData> allConfigEntityData = new ArrayList<>();
        allConfigEntityData.addAll(dataSource.get(EntityData.IDENTITY_PROVIDER));
        allConfigEntityData.addAll(dataSource.get(EntityData.MATCHING_SERVICE));
        allConfigEntityData.addAll(dataSource.get(EntityData.TRANSACTION));
        allConfigEntityData.addAll(dataSource.get(EntityData.TRANSLATIONS));
        allConfigEntityData.addAll(dataSource.get(EntityData.COUNTRIES));

        DuplicateEntityIdConfigValidator duplicateEntityIdConfigValidator = new DuplicateEntityIdConfigValidator();
        duplicateEntityIdConfigValidator.validate(allConfigEntityData);
    };

    private Consumer<Map<EntityData, Collection<? extends ConfigEntityData>>> checkEachTransactionHasCorrespondingEntryInMatchingService = dataSource -> {
        TransactionConfigMatchingServiceValidator transactionConfigMatchingServiceValidator = new TransactionConfigMatchingServiceValidator();
        dataSource.get(EntityData.TRANSACTION)
                .forEach(data -> transactionConfigMatchingServiceValidator
                        .validate((TransactionConfigEntityData)data, matchingServiceConfigEntityDataRepository));
    };

    private Consumer<Map<EntityData, Collection<? extends ConfigEntityData>>> checkEachOnboardingIdentityProviderHasACorrespondingTransaction = dataSource -> {
        IdentityProviderConfigOnboardingTransactionValidator identityProviderConfigOnboardingTransactionValidator = new IdentityProviderConfigOnboardingTransactionValidator(transactionConfigEntityDataRepository);
        dataSource.get(EntityData.IDENTITY_PROVIDER)
                .forEach(data -> identityProviderConfigOnboardingTransactionValidator.validate((IdentityProviderConfigEntityData)data));
    };

    private Consumer<Map<EntityData, Collection<? extends ConfigEntityData>>> checkThereAreNoInvalidCertificates = dataSource -> {
        final ImmutableSet<TransactionConfigEntityData> transactionConfigEntityData = ImmutableSet.copyOf((Collection<TransactionConfigEntityData>)dataSource.get(EntityData.TRANSACTION));

        certificateChainConfigValidator.validate(transactionConfigEntityData, ImmutableSet.copyOf((Collection<MatchingServiceConfigEntityData>)dataSource.get(EntityData.MATCHING_SERVICE)));
    };

    private Consumer<Map<EntityData, Collection<? extends ConfigEntityData>>> checkIdpAndTransactionsHaveValidLevelsOfAssurance = dataSource -> {
        final Collection<IdentityProviderConfigEntityData> enabledIdentityProviders = (Collection<IdentityProviderConfigEntityData>)dataSource.get(EntityData.IDENTITY_PROVIDER);
        enabledIdentityProviders
                .stream()
                .filter(input -> input.isEnabled())
                .collect(Collectors.toList());

        final ImmutableSet<IdentityProviderConfigEntityData> identityProviderConfigEntityData = ImmutableSet.copyOf(enabledIdentityProviders);
        final ImmutableSet<TransactionConfigEntityData> transactionConfigEntityData = ImmutableSet.copyOf((Collection<TransactionConfigEntityData>)dataSource.get(EntityData.TRANSACTION));

        levelsOfAssuranceConfigValidator.validateLevelsOfAssurance(identityProviderConfigEntityData, transactionConfigEntityData);
    };
}
