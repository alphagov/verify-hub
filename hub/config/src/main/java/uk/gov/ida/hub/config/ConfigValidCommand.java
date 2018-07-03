package uk.gov.ida.hub.config;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import uk.gov.ida.hub.config.annotations.CertificateConfigValidator;
import uk.gov.ida.hub.config.data.ConfigDataBootstrap;
import uk.gov.ida.hub.config.data.ConfigDataSource;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.data.FileBackedCountriesConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedIdentityProviderConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedMatchingServiceConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedTransactionConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedTranslationsDataSource;
import uk.gov.ida.hub.config.data.LevelsOfAssuranceConfigValidator;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CountriesConfigEntityData;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.ThrowingCertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.domain.TranslationData;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;
import uk.gov.ida.truststore.TrustStoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigValidCommand extends ConfiguredCommand<ConfigConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigValidCommand.class);

    public ConfigValidCommand() {
        super("validate-config", "Checks if the config is valid");
    }

    @Override
    public void run(Bootstrap<ConfigConfiguration> bootstrap, Namespace namespace, ConfigConfiguration configuration) throws Exception {
        Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(new TypeLiteral<ConfigurationFactoryFactory<IdentityProviderConfigEntityData>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigurationFactoryFactory<MatchingServiceConfigEntityData>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigurationFactoryFactory<TransactionConfigEntityData>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigurationFactoryFactory<TranslationData>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigurationFactoryFactory<CountriesConfigEntityData>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigDataSource<TransactionConfigEntityData>>() {}).to(FileBackedTransactionConfigDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigDataSource<TranslationData>>() {}).to(FileBackedTranslationsDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigDataSource<MatchingServiceConfigEntityData>>() {}).to(FileBackedMatchingServiceConfigDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigDataSource<IdentityProviderConfigEntityData>>() {}).to(FileBackedIdentityProviderConfigDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigDataSource<CountriesConfigEntityData>>() {}).to(FileBackedCountriesConfigDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigEntityDataRepository<TransactionConfigEntityData>>() { }).asEagerSingleton();
                        bind(new TypeLiteral<ConfigEntityDataRepository<TranslationData>>() { }).asEagerSingleton();
                        bind(new TypeLiteral<ConfigEntityDataRepository<CountriesConfigEntityData>>() { }).asEagerSingleton();
                        bind(new TypeLiteral<ConfigEntityDataRepository<MatchingServiceConfigEntityData>>() { }).asEagerSingleton();
                        bind(new TypeLiteral<ConfigEntityDataRepository<IdentityProviderConfigEntityData>>() { }).asEagerSingleton();
                        bind(ConfigConfiguration.class).toInstance(configuration);
                        bind(LevelsOfAssuranceConfigValidator.class).toInstance(new LevelsOfAssuranceConfigValidator());
                        bind(TrustStoreConfiguration.class).to(ConfigConfiguration.class);
                        bind(TrustStoreForCertificateProvider.class);
                        bind(CertificateChainConfigValidator.class)
                                .annotatedWith(CertificateConfigValidator.class)
                                .to(ThrowingCertificateChainConfigValidator.class);
                    }
                });

        ConfigDataBootstrap checkConfigValid = injector.getInstance(ConfigDataBootstrap.class);

        try {
            checkConfigValid.start();
        } catch (ConfigValidationException e) {
            LOG.info(e.getMessage());
            throw e;
        }
    }
}
