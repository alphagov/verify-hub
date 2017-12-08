package uk.gov.ida.hub.config.data;

import com.google.common.collect.ImmutableSet;
import io.dropwizard.lifecycle.Managed;
import uk.gov.ida.hub.config.ConfigEntityData;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CountriesConfigEntityData;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.validators.DuplicateEntityIdConfigValidator;
import uk.gov.ida.hub.config.validators.IdentityProviderConfigOnboardingTransactionValidator;
import uk.gov.ida.hub.config.validators.TransactionConfigMatchingServiceValidator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class ConfigDataBootstrap implements Managed {

    private final ConfigDataSource<IdentityProviderConfigEntityData> identityProviderConfigDataSource;
    private final ConfigDataSource<MatchingServiceConfigEntityData> matchingServiceConfigDataSource;
    private final ConfigDataSource<TransactionConfigEntityData> transactionConfigDataSource;
    private final ConfigDataSource<CountriesConfigEntityData> countriesConfigDataSource;

    private final ConfigEntityDataRepository<IdentityProviderConfigEntityData> identityProviderConfigEntityDataRepository;
    private final ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository;
    private final ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository;
    private final ConfigEntityDataRepository<CountriesConfigEntityData> countriesConfigEntityDataConfigEntityDataRepository;
    private final CertificateChainConfigValidator certificateChainConfigValidator;
    private final LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator;

    @Inject
    public ConfigDataBootstrap(
            ConfigDataSource<IdentityProviderConfigEntityData> identityProviderConfigDataSource,
            ConfigDataSource<MatchingServiceConfigEntityData> matchingServiceConfigDataSource,
            ConfigDataSource<TransactionConfigEntityData> transactionConfigDataSource,
            ConfigDataSource<CountriesConfigEntityData> countriesConfigDataSource,
            ConfigEntityDataRepository<IdentityProviderConfigEntityData> identityProviderConfigEntityDataRepository,
            ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository,
            ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository,
            ConfigEntityDataRepository<CountriesConfigEntityData> countriesConfigEntityDataConfigEntityDataRepository,
            CertificateChainConfigValidator certificateChainConfigValidator,
            LevelsOfAssuranceConfigValidator levelsOfAssuranceConfigValidator) {

        this.identityProviderConfigDataSource = identityProviderConfigDataSource;
        this.matchingServiceConfigDataSource = matchingServiceConfigDataSource;
        this.transactionConfigDataSource = transactionConfigDataSource;
        this.countriesConfigDataSource = countriesConfigDataSource;
        this.identityProviderConfigEntityDataRepository = identityProviderConfigEntityDataRepository;
        this.matchingServiceConfigEntityDataRepository = matchingServiceConfigEntityDataRepository;
        this.transactionConfigEntityDataRepository = transactionConfigEntityDataRepository;
        this.countriesConfigEntityDataConfigEntityDataRepository = countriesConfigEntityDataConfigEntityDataRepository;
        this.certificateChainConfigValidator = certificateChainConfigValidator;
        this.levelsOfAssuranceConfigValidator = levelsOfAssuranceConfigValidator;
    }

    @Override
    public void start() {
        final Collection<IdentityProviderConfigEntityData> identityProviderConfigDataCollection = identityProviderConfigDataSource.loadConfig();
        final Collection<MatchingServiceConfigEntityData> matchingServiceConfigDataCollection = matchingServiceConfigDataSource.loadConfig();
        final Collection<TransactionConfigEntityData> transactionConfigDataCollection = transactionConfigDataSource.loadConfig();
        final Collection<CountriesConfigEntityData> countriesConfigEntityDataCollection = countriesConfigDataSource.loadConfig();

        DuplicateEntityIdConfigValidator duplicateEntityIdConfigValidator = new DuplicateEntityIdConfigValidator();

        Collection<ConfigEntityData> allConfigEntityData = new ArrayList<>();
        allConfigEntityData.addAll(identityProviderConfigDataCollection);
        allConfigEntityData.addAll(matchingServiceConfigDataCollection);
        allConfigEntityData.addAll(transactionConfigDataCollection);
        allConfigEntityData.addAll(countriesConfigEntityDataCollection);

        duplicateEntityIdConfigValidator.validate(allConfigEntityData);

        identityProviderConfigEntityDataRepository.addData(identityProviderConfigDataCollection);
        matchingServiceConfigEntityDataRepository.addData(matchingServiceConfigDataCollection);
        transactionConfigEntityDataRepository.addData(transactionConfigDataCollection);
        countriesConfigEntityDataConfigEntityDataRepository.addData(countriesConfigEntityDataCollection);

        TransactionConfigMatchingServiceValidator transactionConfigMatchingServiceValidator = new TransactionConfigMatchingServiceValidator();
        for (TransactionConfigEntityData transactionConfigData : transactionConfigDataCollection) {
            transactionConfigMatchingServiceValidator.validate(transactionConfigData, matchingServiceConfigEntityDataRepository);
        }

        IdentityProviderConfigOnboardingTransactionValidator identityProviderConfigOnboardingTransactionValidator = new IdentityProviderConfigOnboardingTransactionValidator(transactionConfigEntityDataRepository);
        for (IdentityProviderConfigEntityData identityProviderConfigData : identityProviderConfigDataCollection) {
            identityProviderConfigOnboardingTransactionValidator.validate(identityProviderConfigData);
        }

        final Collection<IdentityProviderConfigEntityData> enabledIdentityProviders = identityProviderConfigDataCollection
                .stream()
                .filter(input -> input.isEnabled())
                .collect(Collectors.toList());
        final ImmutableSet<IdentityProviderConfigEntityData> identityProviderConfigEntityData = ImmutableSet.copyOf(enabledIdentityProviders);
        final ImmutableSet<TransactionConfigEntityData> transactionConfigEntityData = ImmutableSet.copyOf(transactionConfigDataCollection);

        certificateChainConfigValidator.validate(transactionConfigEntityData, ImmutableSet.copyOf(matchingServiceConfigDataCollection));

        levelsOfAssuranceConfigValidator.validateLevelsOfAssurance(identityProviderConfigEntityData, transactionConfigEntityData);
    }

    @Override
    public void stop() {
        // don't need to do anything
    }

}
