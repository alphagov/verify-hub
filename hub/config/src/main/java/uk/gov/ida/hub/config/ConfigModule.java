package uk.gov.ida.hub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.OCSPCertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.OCSPPKIXParametersProvider;
import uk.gov.ida.common.shared.security.verification.PKIXParametersProvider;
import uk.gov.ida.hub.config.annotations.CertificateConfigValidator;
import uk.gov.ida.hub.config.application.CertificateService;
import uk.gov.ida.hub.config.application.MatchingServiceAdapterService;
import uk.gov.ida.hub.config.data.ConfigDataBootstrap;
import uk.gov.ida.hub.config.data.ConfigDataSource;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.data.FileBackedCountriesConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedIdentityProviderConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedMatchingServiceConfigDataSource;
import uk.gov.ida.hub.config.data.FileBackedTransactionConfigDataSource;
import uk.gov.ida.hub.config.data.LevelsOfAssuranceConfigValidator;
import uk.gov.ida.hub.config.domain.CertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.CountriesConfigEntityData;
import uk.gov.ida.hub.config.domain.EntityConfigDataToCertificateDtoTransformer;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LoggingCertificateChainConfigValidator;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.domain.filters.IdpPredicateFactory;
import uk.gov.ida.hub.config.exceptions.ExceptionFactory;
import uk.gov.ida.hub.config.healthcheck.ConfigHealthCheck;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;
import uk.gov.ida.truststore.KeyStoreCache;
import uk.gov.ida.truststore.KeyStoreLoader;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.inject.Inject;

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
        bind(new TypeLiteral<ConfigurationFactoryFactory<IdentityProviderConfigEntityData>>() {}).toInstance(new DefaultConfigurationFactoryFactory<IdentityProviderConfigEntityData>());
        bind(new TypeLiteral<ConfigurationFactoryFactory<TransactionConfigEntityData>>(){}).toInstance(new DefaultConfigurationFactoryFactory<TransactionConfigEntityData>());
        bind(new TypeLiteral<ConfigurationFactoryFactory<MatchingServiceConfigEntityData>>(){}).toInstance(new DefaultConfigurationFactoryFactory<MatchingServiceConfigEntityData>());
        bind(new TypeLiteral<ConfigurationFactoryFactory<CountriesConfigEntityData>>(){}).toInstance(new DefaultConfigurationFactoryFactory<CountriesConfigEntityData>());
        bind(new TypeLiteral<ConfigDataSource<TransactionConfigEntityData>>() {}).to(FileBackedTransactionConfigDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigDataSource<MatchingServiceConfigEntityData>>() {}).to(FileBackedMatchingServiceConfigDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigDataSource<IdentityProviderConfigEntityData>>() {}).to(FileBackedIdentityProviderConfigDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigDataSource<CountriesConfigEntityData>>() {}).to(FileBackedCountriesConfigDataSource.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigEntityDataRepository<TransactionConfigEntityData>>(){}).asEagerSingleton();
        bind(new TypeLiteral<ConfigEntityDataRepository<CountriesConfigEntityData>>(){}).asEagerSingleton();
        bind(new TypeLiteral<ConfigEntityDataRepository<MatchingServiceConfigEntityData>>(){}).asEagerSingleton();
        bind(new TypeLiteral<ConfigEntityDataRepository<IdentityProviderConfigEntityData>>(){}).asEagerSingleton();
        bind(ObjectMapper.class).toInstance(new ObjectMapper().registerModule(new GuavaModule()));
        bind(LevelsOfAssuranceConfigValidator.class).toInstance(new LevelsOfAssuranceConfigValidator());
        bind(CertificateChainValidator.class);
        bind(TrustStoreForCertificateProvider.class);
        bind(X509CertificateFactory.class).toInstance(new X509CertificateFactory());
        bind(KeyStoreCache.class);
        bind(ExceptionFactory.class);
        bind(OCSPCertificateChainValidityChecker.class);
        bind(EntityConfigDataToCertificateDtoTransformer.class);
        bind(OCSPCertificateChainValidator.class);
        bind(IdpPredicateFactory.class);
        bind(KeyStoreLoader.class).toInstance(new KeyStoreLoader());
        bind(OCSPPKIXParametersProvider.class).toInstance(new OCSPPKIXParametersProvider());
        bind(PKIXParametersProvider.class).toInstance(new PKIXParametersProvider());
        bind(CertificateService.class);
        bind(MatchingServiceAdapterService.class);
    }

    @Provides
    public CertificateValidityChecker validityChecker(TrustStoreForCertificateProvider trustStoreForCertificateProvider, CertificateChainValidator certificateChainValidator) {
        return CertificateValidityChecker.createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertificateProvider, certificateChainValidator);
    }

}
