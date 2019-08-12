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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.data.ConfigDataBootstrap;
import uk.gov.ida.hub.config.data.ConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedCountryConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedIdentityProviderConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedMatchingServiceConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedTransactionConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedTranslationsDataSource;
import uk.gov.ida.hub.config.data.LevelsOfAssuranceConfigValidator;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CountryConfig;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.TranslationData;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;
import uk.gov.ida.truststore.TrustStoreConfiguration;

public class ConfigValidCommand extends ConfiguredCommand<ConfigConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigValidCommand.class);

    public ConfigValidCommand() {
        super("validate-config", "Checks if the config is valid");
    }

    @Override
    public void run(Bootstrap<ConfigConfiguration> bootstrap, Namespace namespace, ConfigConfiguration configuration) {
        Injector injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(new TypeLiteral<ConfigurationFactoryFactory<IdentityProviderConfig>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigurationFactoryFactory<MatchingServiceConfig>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigurationFactoryFactory<TransactionConfig>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigurationFactoryFactory<TranslationData>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigurationFactoryFactory<CountryConfig>>() { }).toInstance(new DefaultConfigurationFactoryFactory<>());
                        bind(new TypeLiteral<ConfigDataSource<TransactionConfig>>() {}).to(FileBackedTransactionConfigDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigDataSource<TranslationData>>() {}).to(FileBackedTranslationsDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigDataSource<MatchingServiceConfig>>() {}).to(FileBackedMatchingServiceConfigDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigDataSource<IdentityProviderConfig>>() {}).to(FileBackedIdentityProviderConfigDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<ConfigDataSource<CountryConfig>>() {}).to(FileBackedCountryConfigDataSource.class).asEagerSingleton();
                        bind(new TypeLiteral<LocalConfigRepository<TransactionConfig>>() { }).asEagerSingleton();
                        bind(new TypeLiteral<LocalConfigRepository<TranslationData>>() { }).asEagerSingleton();
                        bind(new TypeLiteral<LocalConfigRepository<CountryConfig>>() { }).asEagerSingleton();
                        bind(new TypeLiteral<LocalConfigRepository<MatchingServiceConfig>>() { }).asEagerSingleton();
                        bind(new TypeLiteral<LocalConfigRepository<IdentityProviderConfig>>() { }).asEagerSingleton();
                        bind(ConfigConfiguration.class).toInstance(configuration);
                        bind(LevelsOfAssuranceConfigValidator.class).toInstance(new LevelsOfAssuranceConfigValidator());
                        bind(TrustStoreConfiguration.class).to(ConfigConfiguration.class);
                        bind(TrustStoreForCertificateProvider.class);
                        bind(CertificateChainConfigValidator.class);
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
