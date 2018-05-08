package uk.gov.ida.hub.samlproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Environment;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.AbstractReloadingMetadataResolver;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.w3c.dom.Element;
import uk.gov.ida.common.IpFromXForwardedForHeader;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.shared.security.PublicKeyFileInputStreamFactory;
import uk.gov.ida.common.shared.security.PublicKeyInputStreamFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.PKIXParametersProvider;
import uk.gov.ida.eventemitter.Configuration;
import uk.gov.ida.eventsink.EventSink;
import uk.gov.ida.eventsink.EventSinkHttpProxy;
import uk.gov.ida.eventsink.EventSinkMessageSender;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.samlproxy.annotations.Config;
import uk.gov.ida.hub.samlproxy.annotations.Policy;
import uk.gov.ida.hub.samlproxy.config.CertificatesConfigProxy;
import uk.gov.ida.hub.samlproxy.config.ConfigServiceKeyStore;
import uk.gov.ida.hub.samlproxy.config.SamlConfiguration;
import uk.gov.ida.hub.samlproxy.config.TrustStoreForCertificateProvider;
import uk.gov.ida.hub.samlproxy.controllogic.SamlMessageSenderHandler;
import uk.gov.ida.hub.samlproxy.exceptions.ExceptionAuditor;
import uk.gov.ida.hub.samlproxy.exceptions.NoKeyConfiguredForEntityExceptionMapper;
import uk.gov.ida.hub.samlproxy.exceptions.SamlProxyApplicationExceptionMapper;
import uk.gov.ida.hub.samlproxy.exceptions.SamlProxyExceptionMapper;
import uk.gov.ida.hub.samlproxy.exceptions.SamlProxySamlTransformationErrorExceptionMapper;
import uk.gov.ida.hub.samlproxy.factories.EidasValidatorFactory;
import uk.gov.ida.hub.samlproxy.handlers.HubAsIdpMetadataHandler;
import uk.gov.ida.hub.samlproxy.handlers.HubAsSpMetadataHandler;
import uk.gov.ida.hub.samlproxy.logging.ExternalCommunicationEventLogger;
import uk.gov.ida.hub.samlproxy.logging.ProtectiveMonitoringLogFormatter;
import uk.gov.ida.hub.samlproxy.logging.ProtectiveMonitoringLogger;
import uk.gov.ida.hub.samlproxy.proxy.SessionProxy;
import uk.gov.ida.hub.samlproxy.security.AuthnRequestKeyStore;
import uk.gov.ida.hub.samlproxy.security.AuthnResponseKeyStore;
import uk.gov.ida.hub.samlproxy.security.HubSigningKeyStore;
import uk.gov.ida.jerseyclient.DefaultClientProvider;
import uk.gov.ida.jerseyclient.ErrorHandlingClient;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.jerseyclient.JsonResponseProcessor;
import uk.gov.ida.restclient.ClientProvider;
import uk.gov.ida.restclient.RestfulClientConfiguration;
import uk.gov.ida.saml.core.InternalPublicKeyStore;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.core.security.RelayStateValidator;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.api.HubTransformersFactory;
import uk.gov.ida.saml.hub.validators.StringSizeValidator;
import uk.gov.ida.saml.hub.validators.response.common.ResponseMaxSizeValidator;
import uk.gov.ida.saml.metadata.EidasMetadataConfiguration;
import uk.gov.ida.saml.metadata.EidasMetadataResolverRepository;
import uk.gov.ida.saml.metadata.EidasTrustAnchorHealthCheck;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;
import uk.gov.ida.saml.metadata.ExpiredCertificateMetadataFilter;
import uk.gov.ida.saml.metadata.HubMetadataPublicKeyStore;
import uk.gov.ida.saml.metadata.IdpMetadataPublicKeyStore;
import uk.gov.ida.saml.metadata.MetadataHealthCheck;
import uk.gov.ida.saml.metadata.MetadataResolverConfigBuilder;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.factories.DropwizardMetadataResolverFactory;
import uk.gov.ida.saml.metadata.factories.MetadataClientFactory;
import uk.gov.ida.saml.metadata.factories.MetadataSignatureTrustEngineFactory;
import uk.gov.ida.saml.security.CredentialFactorySignatureValidator;
import uk.gov.ida.saml.security.PublicKeyFactory;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.saml.security.SigningCredentialFactory;
import uk.gov.ida.saml.security.SigningKeyStore;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;
import uk.gov.ida.shared.utils.IpAddressResolver;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;
import uk.gov.ida.truststore.KeyStoreCache;
import uk.gov.ida.truststore.KeyStoreLoader;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import java.io.PrintWriter;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.Timer;
import java.util.function.Function;

public class SamlProxyModule extends AbstractModule {

    public static final String VERIFY_METADATA_HEALTH_CHECK = "VerifyMetadataHealthCheck";
    public static final String COUNTRY_METADATA_HEALTH_CHECK = "CountryMetadataHealthCheck";

    @Override
    protected void configure() {
        bind(TrustStoreConfiguration.class).to(SamlProxyConfiguration.class);
        bind(RestfulClientConfiguration.class).to(SamlProxyConfiguration.class);
        bind(PublicKeyInputStreamFactory.class).toInstance(new PublicKeyFileInputStreamFactory());
        bind(SigningKeyStore.class).to(AuthnRequestKeyStore.class);
        bind(Client.class).toProvider(DefaultClientProvider.class).in(Scopes.SINGLETON);
        bind(EventSinkProxy.class).to(EventSinkHttpProxy.class);
        bind(ConfigServiceKeyStore.class).asEagerSingleton();
        bind(KeyStoreLoader.class).toInstance(new KeyStoreLoader());
        bind(ResponseMaxSizeValidator.class);
        bind(ExpiredCertificateMetadataFilter.class).toInstance(new ExpiredCertificateMetadataFilter());
        bind(X509CertificateFactory.class).toInstance(new X509CertificateFactory());
        bind(CertificateChainValidator.class);
        bind(CertificatesConfigProxy.class);
        bind(TrustStoreForCertificateProvider.class);
        bind(StringSizeValidator.class).toInstance(new StringSizeValidator());
        bind(JsonResponseProcessor.class);
        bind(ObjectMapper.class).toInstance(new ObjectMapper());
        bind(PKIXParametersProvider.class).toInstance(new PKIXParametersProvider());
        bind(RelayStateValidator.class).toInstance(new RelayStateValidator());
        bind(ProtectiveMonitoringLogFormatter.class).toInstance(new ProtectiveMonitoringLogFormatter());
        bind(KeyStoreCache.class);
        bind(EventSinkMessageSender.class);
        bind(ExceptionAuditor.class);
        bind(ProtectiveMonitoringLogger.class);
        bind(SessionProxy.class);
        bind(new TypeLiteral<LevelLoggerFactory<SamlProxySamlTransformationErrorExceptionMapper>>(){}).toInstance(new LevelLoggerFactory<>());
        bind(new TypeLiteral<LevelLoggerFactory<NoKeyConfiguredForEntityExceptionMapper>>(){}).toInstance(new LevelLoggerFactory<>());
        bind(new TypeLiteral<LevelLoggerFactory<SamlProxyApplicationExceptionMapper>>(){}).toInstance(new LevelLoggerFactory<>());
        bind(new TypeLiteral<LevelLoggerFactory<SamlProxyExceptionMapper>>(){}).toInstance(new LevelLoggerFactory<>());
        bind(SamlMessageSenderHandler.class);
        bind(ExternalCommunicationEventLogger.class);
        bind(IpAddressResolver.class).toInstance(new IpAddressResolver());
    }

    @Provides
    private Optional<Configuration> getEventEmitterConfiguration(final SamlProxyConfiguration configuration) {
        return Optional.ofNullable(configuration.getEventEmitterConfiguration());
    }

    @Provides
    @Singleton
    @Named("VerifyMetadataResolver")
    public MetadataResolver getVerifyMetadataResolver(Environment environment, SamlProxyConfiguration configuration) {
        final MetadataResolver metadataResolver = new DropwizardMetadataResolverFactory().createMetadataResolver(environment, configuration.getMetadataConfiguration());
        registerMetadataRefreshTask(environment, metadataResolver, configuration.getMetadataConfiguration(), "metadata");
        return metadataResolver;
    }

    @Provides
    @Singleton
    @Named(VERIFY_METADATA_HEALTH_CHECK)
    public MetadataHealthCheck getVerifyMetadataHealthCheck(
        @Named("VerifyMetadataResolver") MetadataResolver metadataResolver,
        Environment environment,
        SamlProxyConfiguration configuration) {
        MetadataHealthCheck metadataHealthCheck = new MetadataHealthCheck(metadataResolver, configuration.getMetadataConfiguration().getExpectedEntityId());
        environment.healthChecks().register(VERIFY_METADATA_HEALTH_CHECK, metadataHealthCheck);
        return metadataHealthCheck;
    }

    @Provides
    @Singleton
    public InternalPublicKeyStore getHubMetadataPublicKeyStore(
            @Named("VerifyMetadataResolver") MetadataResolver metadataResolver,
            PublicKeyFactory publicKeyFactory,
            @Named("HubEntityId") String hubEntityId) {
        return new HubMetadataPublicKeyStore(metadataResolver, publicKeyFactory, hubEntityId);
    }

    @Provides
    @Singleton
    public HubAsIdpMetadataHandler getHubAsIdpMetadataHandler(
            @Named("VerifyMetadataResolver") MetadataResolver metadataResolver,
            SamlProxyConfiguration configuration,
            @Named("HubEntityId") String hubEntityId,
            @Named("HubFederationId") String hubFederationId) {
        return new HubAsIdpMetadataHandler(metadataResolver, configuration, hubEntityId, hubFederationId);
    }

    @Provides
    @Singleton
    public HubAsSpMetadataHandler getHubAsIdpMetadataHandler(
            @Named("VerifyMetadataResolver") MetadataResolver metadataResolver,
            SamlProxyConfiguration configuration,
            XmlObjectToBase64EncodedStringTransformer<EntityDescriptor> entityDescriptorElementTransformer,
            StringToOpenSamlObjectTransformer<EntityDescriptor> elementEntityDescriptorTransformer,
            @Named("HubEntityId") String hubEntityId) {
        return new HubAsSpMetadataHandler(
                metadataResolver,
                configuration,
                entityDescriptorElementTransformer,
                elementEntityDescriptorTransformer,
                hubEntityId);
    }

    @Provides
    @Singleton
    public Optional<EidasMetadataResolverRepository> getEidasMetadataResolverRepository(Environment environment, SamlProxyConfiguration configuration){
        if (configuration.isEidasEnabled()){
            EidasMetadataConfiguration eidasMetadataConfiguration = configuration.getCountryConfiguration().get().getMetadataConfiguration();

            URI uri = eidasMetadataConfiguration.getTrustAnchorUri();

            Client client = new ClientProvider(
                    environment,
                    eidasMetadataConfiguration.getJerseyClientConfiguration(),
                    true,
                    eidasMetadataConfiguration.getJerseyClientName()).get();

            KeyStore keystore = eidasMetadataConfiguration.getTrustStore();

            EidasTrustAnchorResolver trustAnchorResolver = new EidasTrustAnchorResolver(uri, client, keystore);

            EidasMetadataResolverRepository metadataResolverRepository = new EidasMetadataResolverRepository(
                    trustAnchorResolver,
                    environment,
                    eidasMetadataConfiguration,
                    new DropwizardMetadataResolverFactory(),
                    new Timer(),
                    new MetadataSignatureTrustEngineFactory(),
                    new MetadataResolverConfigBuilder(),
                    new MetadataClientFactory()
            );
            registerEidasMetadataRefreshTask(environment, metadataResolverRepository,  "eidas-metadata");
            return Optional.of(metadataResolverRepository);
        }
        return Optional.empty();
    }

    @Provides
    @Singleton
    public Optional<EidasValidatorFactory> getEidasValidatorFactory(Optional<EidasMetadataResolverRepository> eidasMetadataResolverRepository){

         return eidasMetadataResolverRepository.map(EidasValidatorFactory::new);
    }

    @Provides
    @Singleton
    @Named(COUNTRY_METADATA_HEALTH_CHECK)
    public Optional<EidasTrustAnchorHealthCheck> getCountryMetadataHealthCheck(
        Optional<EidasMetadataResolverRepository> metadataResolverRepository,
        Environment environment){

        Optional<EidasTrustAnchorHealthCheck> metadataHealthCheck = metadataResolverRepository
                .map(repository -> new EidasTrustAnchorHealthCheck(repository));

        metadataHealthCheck.ifPresent(healthCheck -> environment.healthChecks().register(COUNTRY_METADATA_HEALTH_CHECK, healthCheck));
        return metadataHealthCheck;
    }

    @Provides
    @Singleton
    @Named("HubEntityId")
    public String getHubEntityId(SamlProxyConfiguration configuration) {
        return configuration.getSamlConfiguration().getEntityId();
    }

    @Provides
    @Singleton
    @Named("HubFederationId")
    public String getHubFederationId(SamlProxyConfiguration configuration) {
        return "VERIFY-FEDERATION";
    }

    @Provides
    @Singleton
    public IpFromXForwardedForHeader ipFromXForwardedForHeader() {
        return new IpFromXForwardedForHeader();
    }

    @Provides
    public PublicKeyFactory publicKeyFactory() throws CertificateException {
        return new PublicKeyFactory();
    }

    @Provides
    @Singleton
    public JsonClient jsonClient(JsonResponseProcessor jsonResponseProcessor, Environment environment, SamlProxyConfiguration configuration) {
        Client client = new ClientProvider(
                environment,
                configuration.getJerseyClientConfiguration(),
                configuration.getEnableRetryTimeOutConnections(),
                "samlProxyClient").get();
        ErrorHandlingClient errorHandlingClient = new ErrorHandlingClient(client);
        return new JsonClient(errorHandlingClient, jsonResponseProcessor);
    }

    @Provides
    @Singleton
    private StringToOpenSamlObjectTransformer<Response> getStringToResponseTransformer(ResponseMaxSizeValidator responseMaxSizeValidator) {
        return new HubTransformersFactory().getStringToResponseTransformer(responseMaxSizeValidator);
    }

    @Provides
    @Singleton
    private StringToOpenSamlObjectTransformer<AuthnRequest> getStringToAuthnRequestTransformer() {
        return new HubTransformersFactory().getStringToAuthnRequestTransformer();
    }

    @Provides
    @SuppressWarnings("unused")
    public Function<HubIdentityProviderMetadataDto, Element> getHubIdentityProviderMetadataDtoToElementTransformer() {
        return new HubTransformersFactory().getHubIdentityProviderMetadataDtoToElementTransformer();
    }

    @Provides
    @Singleton
    public XmlObjectToBase64EncodedStringTransformer<EntityDescriptor> entityDescriptorStringTransformer() {
        return new XmlObjectToBase64EncodedStringTransformer<>();
    }

    @Provides
    @Singleton
    public XmlObjectToElementTransformer<EntityDescriptor> entityDescriptorElementTransformer() {
        return new CoreTransformersFactory().<EntityDescriptor>getXmlObjectToElementTransformer();
    }

    @Provides
    @Singleton
    public StringToOpenSamlObjectTransformer<EntityDescriptor> elementEntityDescriptorTransformer() {
        return new CoreTransformersFactory().getStringtoOpenSamlObjectTransformer(input -> { });
    }

    @Provides
    @Named("authnRequestPublicCredentialFactory")
    public SigningCredentialFactory getFactoryForAuthnRequests(ConfigServiceKeyStore configServiceKeyStore) {
        return new SigningCredentialFactory(new AuthnRequestKeyStore(configServiceKeyStore));
    }

    @Provides
    @Singleton
    @Named("VerifyIdpMetadataPublicKeyStore")
    public IdpMetadataPublicKeyStore getVerifyIdpMetadataPublicKeyStore(@Named("VerifyMetadataResolver") MetadataResolver metadataResolver) {
        return new IdpMetadataPublicKeyStore(metadataResolver);
    }

    @Provides
    @Singleton
    @Named("authnResponsePublicCredentialFactory")
    public SigningCredentialFactory getFactoryForAuthnResponses(@Named("VerifyIdpMetadataPublicKeyStore") IdpMetadataPublicKeyStore idpMetadataPublicKeyStore) {
        return new SigningCredentialFactory(new AuthnResponseKeyStore(idpMetadataPublicKeyStore));
    }

    @Provides
    @Named("hubSigningCredentialFactory")
    public SigningCredentialFactory getFactoryForHubSigning(InternalPublicKeyStore internalPublicKeyStore) {
        return new SigningCredentialFactory(new HubSigningKeyStore(internalPublicKeyStore));
    }

    @Provides
    public SamlMessageSignatureValidator getSamlMessageSignatureValidator(@Named("hubSigningCredentialFactory") SigningCredentialFactory signingCredentialFactory) {
        return new SamlMessageSignatureValidator(new CredentialFactorySignatureValidator(signingCredentialFactory));
    }

    @Named("authnRequestSignatureValidator")
    @Provides
    @Singleton
    public SamlMessageSignatureValidator getAuthnRequestSignatureValidator(@Named("authnRequestPublicCredentialFactory") SigningCredentialFactory signingCredentialFactory) {
        return new SamlMessageSignatureValidator(new CredentialFactorySignatureValidator(signingCredentialFactory));
    }

    @Named("authnResponseSignatureValidator")
    @Provides
    @Singleton
    public SamlMessageSignatureValidator getAuthnResponseSignatureValidator(@Named("authnResponsePublicCredentialFactory") SigningCredentialFactory signingCredentialFactory) {
        return new SamlMessageSignatureValidator(new CredentialFactorySignatureValidator(signingCredentialFactory));
    }

    @Provides
    @Config
    public URI configUri(SamlProxyConfiguration policyConfiguration) {
        return policyConfiguration.getConfigUri();
    }

    @Provides
    @EventSink
    public URI eventSinkUri(SamlProxyConfiguration policyConfiguration) {
        return policyConfiguration.getEventSinkUri();
    }

    @Provides
    @Policy
    public URI policyUri(SamlProxyConfiguration samlProxyConfiguration) {
        return samlProxyConfiguration.getPolicyUri();
    }

    @Provides
    public SamlConfiguration samlConfiguration(SamlProxyConfiguration samlProxyConfiguration) {
        return samlProxyConfiguration.getSamlConfiguration();
    }

    @Provides
    public ServiceInfoConfiguration serviceInfoConfiguration(SamlProxyConfiguration samlProxyConfiguration) {
        return samlProxyConfiguration.getServiceInfo();
    }

    private void registerMetadataRefreshTask(Environment environment, MetadataResolver metadataResolver, MetadataResolverConfiguration metadataResolverConfiguration, String name) {
        environment.admin().addTask(new Task(name + "-refresh") {
            @Override
            public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
                ((AbstractReloadingMetadataResolver) metadataResolver).refresh();
            }
        });
    }

    private void registerEidasMetadataRefreshTask(Environment environment, EidasMetadataResolverRepository eidasMetadataResolverRepository, String name){
        environment.admin().addTask(new Task(name + "-refresh") {
            @Override
            public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) {
                eidasMetadataResolverRepository.refresh();
            }
        });
    }
}
