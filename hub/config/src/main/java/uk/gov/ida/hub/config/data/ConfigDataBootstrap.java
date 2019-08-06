package uk.gov.ida.hub.config.data;

import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CountryConfig;
import uk.gov.ida.hub.config.domain.EntityIdentifiable;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigDataBootstrap implements Managed {

    private final ConfigDataSource<IdentityProviderConfig> identityProviderConfigDataSource;
    private final ConfigDataSource<MatchingServiceConfig> matchingServiceConfigDataSource;
    private final ConfigDataSource<TransactionConfig> transactionConfigDataSource;
    private final ConfigDataSource<TranslationData> translationsDataSource;
    private final ConfigDataSource<CountryConfig> countriesConfigDataSource;

    private final LocalConfigRepository<IdentityProviderConfig> identityProviderConfigRepository;
    private final LocalConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository;
    private final LocalConfigRepository<TransactionConfig> transactionConfigRepository;
    private final LocalConfigRepository<TranslationData> translationsRepository;
    private final LocalConfigRepository<CountryConfig> countryConfigRepository;
    private final CertificateChainConfigValidator certificateChainConfigValidator;
    private final LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator;

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
            CertificateChainConfigValidator certificateChainConfigValidator,
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
        cacheConfigData();

        checkForDuplicateEntityIds();
        checkEachTransactionHasCorrespondingEntryInMatchingService();
        checkEachOnboardingIdentityProviderHasACorrespondingTransaction();
        checkThereAreNoInvalidCertificates();
        checkIdpAndTransactionsHaveValidLevelsOfAssurance();
    }

    @Override
    public void stop() {
        // don't need to do anything
    }

    private void cacheConfigData() {
        identityProviderConfigRepository.addData(identityProviderConfigDataSource.loadConfig());
        matchingServiceConfigRepository.addData(matchingServiceConfigDataSource.loadConfig());
        transactionConfigRepository.addData(transactionConfigDataSource.loadConfig());
        countryConfigRepository.addData(countriesConfigDataSource.loadConfig());
        translationsRepository.addData(translationsDataSource.loadConfig());
    }

    private void checkForDuplicateEntityIds(){
        Collection<EntityIdentifiable> allConfigEntityData = new ArrayList<>();
        allConfigEntityData.addAll(identityProviderConfigRepository.getAllData());
        allConfigEntityData.addAll(matchingServiceConfigRepository.getAllData());
        allConfigEntityData.addAll(transactionConfigRepository.getAllData());
        allConfigEntityData.addAll(countryConfigRepository.getAllData());
        allConfigEntityData.addAll(translationsRepository.getAllData());

        new DuplicateEntityIdConfigValidator()
                .validate(allConfigEntityData);
    }

    private void checkEachTransactionHasCorrespondingEntryInMatchingService() {
        TransactionConfigMatchingServiceValidator validator = new TransactionConfigMatchingServiceValidator(matchingServiceConfigRepository);
        transactionConfigRepository.getAllData().forEach(validator::validate);
    }

    private void checkEachOnboardingIdentityProviderHasACorrespondingTransaction() {
        IdentityProviderConfigOnboardingTransactionValidator validator = new IdentityProviderConfigOnboardingTransactionValidator(transactionConfigRepository);
        identityProviderConfigRepository.getAllData()
                .forEach(idpConfig -> validator.validate(idpConfig));
    }

    private void checkThereAreNoInvalidCertificates() {
        var configs = Stream.concat(
                transactionConfigRepository.getAllData().stream(),
                matchingServiceConfigRepository.getAllData().stream())
                .collect(Collectors.toSet());

        certificateChainConfigValidator.validate(configs);
    }

    private void checkIdpAndTransactionsHaveValidLevelsOfAssurance() {
        final Set<IdentityProviderConfig> enabledIdentityProviders = identityProviderConfigRepository
                .getAllData()
                .stream()
                .filter(input -> input.isEnabled())
                .collect(Collectors.toSet());

        Set<TransactionConfig> transactionConfigs = transactionConfigRepository.getAllData();
        levelsOfAssuranceConfigValidator.validateLevelsOfAssurance(enabledIdentityProviders, transactionConfigs);
    }

}
