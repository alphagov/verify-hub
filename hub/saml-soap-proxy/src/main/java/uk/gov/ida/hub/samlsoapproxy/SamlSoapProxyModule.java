package uk.gov.ida.hub.samlsoapproxy;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.UrlConfigurationSourceProvider;
import io.dropwizard.setup.Environment;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.w3c.dom.Element;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.shared.security.PublicKeyFileInputStreamFactory;
import uk.gov.ida.common.shared.security.PublicKeyInputStreamFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.PKIXParametersProvider;
import uk.gov.ida.eventemitter.Configuration;
import uk.gov.ida.eventsink.EventSink;
import uk.gov.ida.eventsink.EventSinkHttpProxy;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.samlsoapproxy.annotations.Config;
import uk.gov.ida.hub.samlsoapproxy.annotations.MatchingServiceRequestExecutorBacklog;
import uk.gov.ida.hub.samlsoapproxy.annotations.Policy;
import uk.gov.ida.hub.samlsoapproxy.annotations.SamlEngine;
import uk.gov.ida.hub.samlsoapproxy.client.AttributeQueryRequestClient;
import uk.gov.ida.hub.samlsoapproxy.client.HealthCheckSoapRequestClient;
import uk.gov.ida.hub.samlsoapproxy.client.MatchingServiceHealthCheckClient;
import uk.gov.ida.hub.samlsoapproxy.client.SoapRequestClient;
import uk.gov.ida.hub.samlsoapproxy.config.ConfigProxy;
import uk.gov.ida.hub.samlsoapproxy.config.ConfigServiceKeyStore;
import uk.gov.ida.hub.samlsoapproxy.config.MatchingServiceAdapterMetadataRetriever;
import uk.gov.ida.hub.samlsoapproxy.config.SamlConfiguration;
import uk.gov.ida.hub.samlsoapproxy.config.TrustStoreForCertificateProvider;
import uk.gov.ida.hub.samlsoapproxy.domain.TimeoutEvaluator;
import uk.gov.ida.hub.samlsoapproxy.health.MetadataHealthCheckRegistry;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckHandler;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthChecker;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.SupportedMsaVersions;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.SupportedMsaVersionsBootstrap;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.SupportedMsaVersionsLoader;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.SupportedMsaVersionsRepository;
import uk.gov.ida.hub.samlsoapproxy.logging.ExternalCommunicationEventLogger;
import uk.gov.ida.hub.samlsoapproxy.logging.HealthCheckEventLogger;
import uk.gov.ida.hub.samlsoapproxy.logging.ProtectiveMonitoringLogger;
import uk.gov.ida.hub.samlsoapproxy.proxy.HubMatchingServiceResponseReceiverProxy;
import uk.gov.ida.hub.samlsoapproxy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.samlsoapproxy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.samlsoapproxy.runnabletasks.AttributeQueryRequestRunnableFactory;
import uk.gov.ida.hub.samlsoapproxy.runnabletasks.ExecuteAttributeQueryRequest;
import uk.gov.ida.hub.samlsoapproxy.security.MatchingResponseSigningKeyStore;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.jerseyclient.DefaultClientProvider;
import uk.gov.ida.jerseyclient.ErrorHandlingClient;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.jerseyclient.JsonResponseProcessor;
import uk.gov.ida.restclient.ClientProvider;
import uk.gov.ida.restclient.RestfulClientConfiguration;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.metadata.ExpiredCertificateMetadataFilter;
import uk.gov.ida.saml.metadata.MetadataHealthCheck;
import uk.gov.ida.saml.metadata.MetadataRefreshTask;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;
import uk.gov.ida.saml.metadata.factories.DropwizardMetadataResolverFactory;
import uk.gov.ida.saml.security.CredentialFactorySignatureValidator;
import uk.gov.ida.saml.security.MetadataBackedSignatureValidator;
import uk.gov.ida.saml.security.PublicKeyFactory;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.saml.security.SigningCredentialFactory;
import uk.gov.ida.shared.utils.IpAddressResolver;
import uk.gov.ida.truststore.KeyStoreCache;
import uk.gov.ida.truststore.KeyStoreLoader;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import java.net.URI;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

public class SamlSoapProxyModule extends AbstractModule {


    public static final String VERIFY_METADATA_TRUST_ENGINE = "VERIFY_METADATA_TRUST_ENGINE";

    public SamlSoapProxyModule() {
    }

    @Override
    protected void configure() {
        bind(TrustStoreConfiguration.class).to(SamlSoapProxyConfiguration.class);
        bind(EventSinkProxy.class).to(EventSinkHttpProxy.class);
        bind(PublicKeyInputStreamFactory.class).toInstance(new PublicKeyFileInputStreamFactory());
        bind(RestfulClientConfiguration.class).to(SamlSoapProxyConfiguration.class);
        bind(Client.class).toProvider(DefaultClientProvider.class).asEagerSingleton();
        bind(new TypeLiteral<ConfigurationFactoryFactory<SupportedMsaVersions>>() {}).toInstance(new DefaultConfigurationFactoryFactory<SupportedMsaVersions>() {});
        bind(new TypeLiteral<SupportedMsaVersionsRepository>() {}).asEagerSingleton();
        bind(SupportedMsaVersionsBootstrap.class).asEagerSingleton();
        bind(SupportedMsaVersionsLoader.class).asEagerSingleton();
        bind(MetadataRefreshTask.class).asEagerSingleton();
        bind(ConfigServiceKeyStore.class).asEagerSingleton();
        bind(ExpiredCertificateMetadataFilter.class).toInstance(new ExpiredCertificateMetadataFilter());
        bind(UrlConfigurationSourceProvider.class).toInstance(new UrlConfigurationSourceProvider());
        bind(TrustStoreForCertificateProvider.class);
        bind(JsonResponseProcessor.class);
        bind(X509CertificateFactory.class).toInstance(new X509CertificateFactory());
        bind(CertificateChainValidator.class);
        bind(ConfigProxy.class);
        bind(PKIXParametersProvider.class).toInstance(new PKIXParametersProvider());
        bind(KeyStoreCache.class);
        bind(KeyStoreLoader.class).toInstance(new KeyStoreLoader());
        bind(MatchingServiceHealthCheckHandler.class);
        bind(MatchingServiceHealthChecker.class);
        bind(MatchingServiceConfigProxy.class);
        bind(MatchingServiceHealthCheckClient.class);
        bind(HealthCheckEventLogger.class);
        bind(SamlEngineProxy.class);
        bind(HealthCheckSoapRequestClient.class);
        bind(AttributeQueryRequestRunnableFactory.class);
        bind(ExecuteAttributeQueryRequest.class);
        bind(AttributeQueryRequestClient.class);
        bind(ProtectiveMonitoringLogger.class).toInstance(new ProtectiveMonitoringLogger());
        bind(SoapRequestClient.class);
        bind(HubMatchingServiceResponseReceiverProxy.class);
        bind(ExternalCommunicationEventLogger.class);
        bind(SoapMessageManager.class).toInstance(new SoapMessageManager());
        bind(IpAddressResolver.class).toInstance(new IpAddressResolver());
        bind(TimeoutEvaluator.class).toInstance(new TimeoutEvaluator());
        bind(MetadataHealthCheckRegistry.class).asEagerSingleton();
        bind(MatchingServiceAdapterMetadataRetriever.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    private DropwizardMetadataResolverFactory getDropwizardMetadataResolverFactory() {
        return new DropwizardMetadataResolverFactory();
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private ObjectMapper getObjectMapper(Environment environment) {
        return environment.getObjectMapper();
    }

    @Provides
    private Configuration getEventEmitterConfiguration(final SamlSoapProxyConfiguration configuration) {
        return configuration.getEventEmitterConfiguration();
    }


    @Provides
    @Singleton
    public MetadataHealthCheck getMetadataHealthCheck(MetadataResolver metadataResolver, SamlSoapProxyConfiguration configuration) {
        return new MetadataHealthCheck(metadataResolver, configuration.getMetadataConfiguration().getExpectedEntityId());
    }

    @Provides
    @Singleton
    @Named("HubEntityId")
    public String getHubEntityId(SamlSoapProxyConfiguration configuration) {
        return configuration.getSamlConfiguration().getEntityId();
    }

    @Provides
    public PublicKeyFactory publicKeyFactory() throws CertificateException {
        return new PublicKeyFactory();
    }

    @Provides
    @Singleton
    public JsonClient jsonClient(JsonResponseProcessor jsonResponseProcessor, Environment environment, SamlSoapProxyConfiguration configuration) {
        Client client = new ClientProvider(
                environment,
                configuration.getJerseyClientConfiguration(),
                configuration.getEnableRetryTimeOutConnections(),
                "samlSoapProxyClient").get();
        ErrorHandlingClient errorHandlingClient = new ErrorHandlingClient(client);
        return new JsonClient(errorHandlingClient, jsonResponseProcessor);
    }

    @Provides
    @Singleton
    @Named("SoapClient")
    private Client soapClientProvider(Environment environment, SamlSoapProxyConfiguration samlSoapProxyConfiguration) {
        return new ClientProvider(
                environment,
                samlSoapProxyConfiguration.getSoapJerseyClientConfiguration(),
                samlSoapProxyConfiguration.getEnableRetryTimeOutConnections(),
                "SoapClient")
                .get();
    }

    @Provides
    @Singleton
    @Named("HealthCheckClient")
    private Client healthCheckClientProvider(Environment environment, SamlSoapProxyConfiguration samlSoapProxyConfiguration) {
        return new ClientProvider(
                environment,
                samlSoapProxyConfiguration.getHealthCheckSoapHttpClient(),
                samlSoapProxyConfiguration.getEnableRetryTimeOutConnections(),
                "HealthCheckSoapClient")
                .get();
    }

    @Provides
    @SuppressWarnings("unused")
    private Function<Element, AttributeQuery> getElementToAttributeQueryTransformer() {
        return new CoreTransformersFactory().getElementToOpenSamlXmlObjectTransformer();
    }

    @Provides
    @SuppressWarnings("unused")
    private Function<Element, Response> getElementToResponseTransformer() {
        return new CoreTransformersFactory().getElementToOpenSamlXmlObjectTransformer();
    }

    @Named("matchingRequestSignatureValidator")
    @Provides
    public SamlMessageSignatureValidator getMatchingRequestSignatureValidator(@Named(VERIFY_METADATA_TRUST_ENGINE) ExplicitKeySignatureTrustEngine signatureTrustEngine) {
        return new SamlMessageSignatureValidator(MetadataBackedSignatureValidator.withoutCertificateChainValidation(signatureTrustEngine));
    }

    @Provides
    @Named("matchingResponseSigningCredentialFactory")
    public SigningCredentialFactory getFactoryForAuthnResponses(ConfigServiceKeyStore configServiceKeyStore) {
        return new SigningCredentialFactory(new MatchingResponseSigningKeyStore(configServiceKeyStore));
    }

    @Named("matchingResponseSignatureValidator")
    @Provides
    public SamlMessageSignatureValidator getMatchingResponseSignatureValidator(@Named("matchingResponseSigningCredentialFactory") SigningCredentialFactory signingCredentialFactory) {
        return new SamlMessageSignatureValidator(new CredentialFactorySignatureValidator(signingCredentialFactory));
    }

    @Provides
    @Singleton
    public SamlConfiguration samlConfiguration(SamlSoapProxyConfiguration samlSoapProxyConfiguration) {
        return samlSoapProxyConfiguration.getSamlConfiguration();
    }

    @Provides
    @Singleton
    public ServiceInfoConfiguration serviceInfoConfiguration(SamlSoapProxyConfiguration samlSoapProxyConfiguration) {
        return samlSoapProxyConfiguration.getServiceInfo();
    }

    @Provides
    @Singleton
    public MetricRegistry metricRegistry(Environment environment) {
        return environment.metrics();
    }

    @Provides
    @Config
    @Singleton
    public URI configUri(SamlSoapProxyConfiguration policyConfiguration) {
        return policyConfiguration.getConfigUri();
    }

    @Provides
    @EventSink
    @Singleton
    public URI eventSinkUri(SamlSoapProxyConfiguration policyConfiguration) {
        return policyConfiguration.getEventSinkUri();
    }

    @Provides
    @SamlEngine
    @Singleton
    public URI samlEngineUri(SamlSoapProxyConfiguration policyConfiguration) {
        return policyConfiguration.getSamlEngineUri();
    }

    @Provides
    @Policy
    @Singleton
    public URI policyUri(SamlSoapProxyConfiguration samlProxyConfiguration) {
        return samlProxyConfiguration.getPolicyUri();
    }

    @Provides @Singleton
    public ExecutorService getMatchingServiceExecutorService(Environment environment, SamlSoapProxyConfiguration configuration) {
        return environment.lifecycle()
                .executorService("Matching service caller %s")
                .maxThreads(configuration.getMatchingServiceExecutor().getMaxPoolSize())
                .minThreads(configuration.getMatchingServiceExecutor().getCorePoolSize())
                .keepAliveTime(configuration.getMatchingServiceExecutor().getKeepAliveDuration())
                .build();
    }


    @Provides @Singleton @MatchingServiceRequestExecutorBacklog
    public Counter matchingServiceRequestExecutorBacklogCounter(Environment environment) {
        return environment.metrics().counter("executor-backlog");
    }

    @Provides
    @Singleton
    public MetadataResolverConfiguration metadataConfiguration(SamlSoapProxyConfiguration samlSoapProxyConfiguration) {
        return samlSoapProxyConfiguration.getMetadataConfiguration();
    }
}
