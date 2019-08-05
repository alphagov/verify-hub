package uk.gov.ida.hub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.setup.Environment;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.OCSPCertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.OCSPPKIXParametersProvider;
import uk.gov.ida.common.shared.security.verification.PKIXParametersProvider;
import uk.gov.ida.hub.config.annotations.CertificateConfigValidator;
import uk.gov.ida.hub.config.application.CertificateService;
import uk.gov.ida.hub.config.application.PrometheusClientService;
import uk.gov.ida.hub.config.data.ConfigDataBootstrap;
import uk.gov.ida.hub.config.data.ConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedCountryConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedIdentityProviderConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedMatchingServiceConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedTransactionConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedTranslationsDataSource;
import uk.gov.ida.hub.config.data.LevelsOfAssuranceConfigValidator;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.CountryConfig;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LoggingCertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.TranslationData;
import uk.gov.ida.hub.config.domain.filters.IdpPredicateFactory;
import uk.gov.ida.hub.config.exceptions.ExceptionFactory;
import uk.gov.ida.hub.config.healthcheck.ConfigHealthCheck;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;
import uk.gov.ida.truststore.KeyStoreCache;
import uk.gov.ida.truststore.KeyStoreLoader;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;

public class ConfigModule extends AbstractModule {

    @Inject
    public ConfigModule() {
    }

    @Override
    protected void configure() {
        bind(ConfigHealthCheck.class).asEagerSingleton();
        bind(ConfigDataBootstrap.class).asEagerSingleton();
        bind(CertificateChainConfigValidator.class)
                .annotatedWith(CertificateConfigValidator.class)
                .to(LoggingCertificateChainConfigValidator.class);
        bind(TrustStoreConfiguration.class).to(ConfigConfiguration.class);
        bind(new TypeLiteral<ConfigurationFactoryFactory<IdentityProviderConfig>>() {}).toInstance(new DefaultConfigurationFactoryFactory<>());
        bind(new TypeLiteral<ConfigurationFactoryFactory<TransactionConfig>>(){}).toInstance(new DefaultConfigurationFactoryFactory<>());
        bind(new TypeLiteral<ConfigurationFactoryFactory<MatchingServiceConfig>>(){}).toInstance(new DefaultConfigurationFactoryFactory<>());
        bind(new TypeLiteral<ConfigurationFactoryFactory<CountryConfig>>(){}).toInstance(new DefaultConfigurationFactoryFactory<>());
        bind(new TypeLiteral<ConfigurationFactoryFactory<TranslationData>>(){}).toInstance(new DefaultConfigurationFactoryFactory<>());
        bind(new TypeLiteral<ConfigDataSource<TransactionConfig>>() {}).to(FileBackedTransactionConfigDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigDataSource<TranslationData>>() {}).to(FileBackedTranslationsDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigDataSource<MatchingServiceConfig>>() {}).to(FileBackedMatchingServiceConfigDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigDataSource<IdentityProviderConfig>>() {}).to(FileBackedIdentityProviderConfigDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigDataSource<CountryConfig>>() {}).to(FileBackedCountryConfigDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<LocalConfigRepository<TransactionConfig>>(){}).asEagerSingleton();
        bind(new TypeLiteral<LocalConfigRepository<TranslationData>>(){}).asEagerSingleton();
        bind(new TypeLiteral<LocalConfigRepository<CountryConfig>>(){}).asEagerSingleton();
        bind(new TypeLiteral<LocalConfigRepository<MatchingServiceConfig>>(){}).asEagerSingleton();
        bind(new TypeLiteral<LocalConfigRepository<IdentityProviderConfig>>(){}).asEagerSingleton();
        bind(new TypeLiteral<ManagedEntityConfigRepository<TransactionConfig>>(){}).asEagerSingleton();
        bind(new TypeLiteral<ManagedEntityConfigRepository<MatchingServiceConfig>>(){}).asEagerSingleton();
        bind(LevelsOfAssuranceConfigValidator.class).toInstance(new LevelsOfAssuranceConfigValidator());
        bind(CertificateChainValidator.class);
        bind(TrustStoreForCertificateProvider.class);
        bind(X509CertificateFactory.class).toInstance(new X509CertificateFactory());
        bind(KeyStoreCache.class);
        bind(ExceptionFactory.class);
        bind(OCSPCertificateChainValidityChecker.class);
        bind(OCSPCertificateChainValidator.class);
        bind(IdpPredicateFactory.class);
        bind(KeyStoreLoader.class).toInstance(new KeyStoreLoader());
        bind(OCSPPKIXParametersProvider.class).toInstance(new OCSPPKIXParametersProvider());
        bind(PKIXParametersProvider.class).toInstance(new PKIXParametersProvider());
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private CertificateService getCertificateService(ManagedEntityConfigRepository<TransactionConfig> connectedServiceConfigRepository,
                                                     ManagedEntityConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository,
                                                     CertificateValidityChecker certificateValidityChecker){
        return new CertificateService(connectedServiceConfigRepository, matchingServiceConfigRepository, certificateValidityChecker);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private PrometheusClientService getPrometheusClientService(
        Environment environment,
        ConfigConfiguration configConfiguration,
        CertificateService certificateService,
        OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker) {

        PrometheusClientService prometheusClientService = new PrometheusClientService(
            environment,
            configConfiguration,
            certificateService,
            ocspCertificateChainValidityChecker);
        prometheusClientService.createCertificateExpiryDateCheckMetrics();
        prometheusClientService.createCertificateOcspRevocationStatusCheckMetrics();
        return prometheusClientService;
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private ObjectMapper getObjectMapper(Environment environment) {
        return environment.getObjectMapper();
    }

    @Provides
    @SuppressWarnings("unused")
    public CertificateValidityChecker validityChecker(TrustStoreForCertificateProvider trustStoreForCertificateProvider, CertificateChainValidator certificateChainValidator) {
        return CertificateValidityChecker.createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertificateProvider, certificateChainValidator);
    }

}
