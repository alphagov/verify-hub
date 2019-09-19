package uk.gov.ida.hub.samlengine;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableMultimap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Environment;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.masterslave.MasterSlave;
import io.lettuce.core.masterslave.StatefulRedisMasterSlaveConnection;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.joda.time.DateTime;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.AbstractReloadingMetadataResolver;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.security.crypto.KeySupport;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.w3c.dom.Element;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.hub.samlengine.annotations.Config;
import uk.gov.ida.hub.samlengine.attributequery.AttributeQueryGenerator;
import uk.gov.ida.hub.samlengine.attributequery.HubAttributeQueryRequestBuilder;
import uk.gov.ida.hub.samlengine.attributequery.HubEidasAttributeQueryRequestBuilder;
import uk.gov.ida.hub.samlengine.config.ConfigServiceKeyStore;
import uk.gov.ida.hub.samlengine.config.RedisConfiguration;
import uk.gov.ida.hub.samlengine.config.SamlConfiguration;
import uk.gov.ida.hub.samlengine.exceptions.InvalidConfigurationException;
import uk.gov.ida.hub.samlengine.exceptions.KeyLoadingException;
import uk.gov.ida.hub.samlengine.exceptions.SamlEngineExceptionMapper;
import uk.gov.ida.hub.samlengine.factories.OutboundResponseFromHubToResponseTransformerFactory;
import uk.gov.ida.hub.samlengine.locators.AssignableEntityToEncryptForLocator;
import uk.gov.ida.hub.samlengine.logging.IdpAssertionMetricsCollector;
import uk.gov.ida.hub.samlengine.metadata.SigningCertFromMetadataExtractor;
import uk.gov.ida.hub.samlengine.proxy.CountrySingleSignOnServiceHelper;
import uk.gov.ida.hub.samlengine.proxy.IdpSingleSignOnServiceHelper;
import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.samlengine.redis.AssertionExpirationCacheRedisCodec;
import uk.gov.ida.hub.samlengine.redis.AuthnRequestExpirationCacheRedisCodec;
import uk.gov.ida.hub.samlengine.security.RedisIdExpirationCache;
import uk.gov.ida.hub.samlengine.services.CountryAuthnRequestGeneratorService;
import uk.gov.ida.hub.samlengine.services.CountryAuthnResponseTranslatorService;
import uk.gov.ida.hub.samlengine.services.CountryMatchingServiceRequestGeneratorService;
import uk.gov.ida.hub.samlengine.services.EidasAuthnRequestTranslator;
import uk.gov.ida.hub.samlengine.services.IdaAuthnRequestTranslator;
import uk.gov.ida.hub.samlengine.services.IdpAuthnRequestGeneratorService;
import uk.gov.ida.hub.samlengine.services.IdpAuthnResponseTranslatorService;
import uk.gov.ida.hub.samlengine.services.MatchingServiceHealthcheckRequestGeneratorService;
import uk.gov.ida.hub.samlengine.services.MatchingServiceHealthcheckResponseTranslatorService;
import uk.gov.ida.hub.samlengine.services.MatchingServiceRequestGeneratorService;
import uk.gov.ida.hub.samlengine.services.MatchingServiceResponseTranslatorService;
import uk.gov.ida.hub.samlengine.services.RpAuthnRequestTranslatorService;
import uk.gov.ida.hub.samlengine.services.RpAuthnResponseGeneratorService;
import uk.gov.ida.hub.samlengine.services.RpErrorResponseGeneratorService;
import uk.gov.ida.hub.samlengine.validation.country.EidasAttributeStatementAssertionValidator;
import uk.gov.ida.hub.samlengine.validation.country.EidasAuthnResponseIssuerValidator;
import uk.gov.ida.hub.samlengine.validation.country.ResponseAssertionsFromCountryValidator;
import uk.gov.ida.hub.samlengine.validation.country.ResponseFromCountryValidator;
import uk.gov.ida.jerseyclient.DefaultClientProvider;
import uk.gov.ida.jerseyclient.ErrorHandlingClient;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.jerseyclient.JsonResponseProcessor;
import uk.gov.ida.restclient.ClientProvider;
import uk.gov.ida.restclient.RestfulClientConfiguration;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.core.transformers.AuthnContextFactory;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionBlobEncrypter;
import uk.gov.ida.saml.core.transformers.outbound.decorators.AssertionEncrypter;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseAssertionSigner;
import uk.gov.ida.saml.core.validation.assertion.AssertionAttributeStatementValidator;
import uk.gov.ida.saml.core.validation.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.core.validation.subjectconfirmation.AssertionSubjectConfirmationValidator;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.core.validators.assertion.AuthnStatementAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.DuplicateAssertionValidatorImpl;
import uk.gov.ida.saml.core.validators.subject.AssertionSubjectValidator;
import uk.gov.ida.saml.deserializers.ElementToOpenSamlXMLObjectTransformer;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.api.HubTransformersFactory;
import uk.gov.ida.saml.hub.configuration.SamlAuthnRequestValidityDurationConfiguration;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.HubAttributeQueryRequest;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.MatchingServiceHealthCheckRequest;
import uk.gov.ida.saml.hub.transformers.inbound.AuthnRequestToIdaRequestFromRelyingPartyTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.CountryAuthenticationStatusUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.InboundResponseFromIdpDataGenerator;
import uk.gov.ida.saml.hub.transformers.inbound.PassthroughAssertionUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToCountryAuthenticationStatusCodeMapper;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.AssertionFromIdpToAssertionTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.EncryptedAssertionUnmarshaller;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunctionSHA256;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunctionSHA256;
import uk.gov.ida.saml.hub.transformers.outbound.SimpleProfileOutboundResponseFromHubToSamlResponseTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.SimpleProfileTransactionIdaStatusMarshaller;
import uk.gov.ida.saml.hub.transformers.outbound.providers.ResponseToUnsignedStringTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.providers.SimpleProfileOutboundResponseFromHubToResponseTransformerProvider;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIdKey;
import uk.gov.ida.saml.hub.validators.authnrequest.ConcurrentMapIdExpirationCache;
import uk.gov.ida.saml.hub.validators.authnrequest.IdExpirationCache;
import uk.gov.ida.saml.metadata.EidasMetadataConfiguration;
import uk.gov.ida.saml.metadata.EidasMetadataResolverRepository;
import uk.gov.ida.saml.metadata.EidasTrustAnchorHealthCheck;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;
import uk.gov.ida.saml.metadata.ExpiredCertificateMetadataFilter;
import uk.gov.ida.saml.metadata.MetadataResolverConfigBuilder;
import uk.gov.ida.saml.metadata.factories.DropwizardMetadataResolverFactory;
import uk.gov.ida.saml.metadata.factories.MetadataSignatureTrustEngineFactory;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.DecrypterFactory;
import uk.gov.ida.saml.security.EidasValidatorFactory;
import uk.gov.ida.saml.security.EncrypterFactory;
import uk.gov.ida.saml.security.EncryptionKeyStore;
import uk.gov.ida.saml.security.EntityToEncryptForLocator;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;
import uk.gov.ida.saml.security.KeyStoreBackedEncryptionCredentialResolver;
import uk.gov.ida.saml.security.MetadataBackedSignatureValidator;
import uk.gov.ida.saml.security.SecretKeyEncrypter;
import uk.gov.ida.saml.security.SigningKeyStore;
import uk.gov.ida.saml.security.validators.encryptedelementtype.EncryptionAlgorithmValidator;
import uk.gov.ida.saml.security.validators.issuer.IssuerValidator;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;
import uk.gov.ida.shared.dropwizard.infinispan.util.InfinispanCacheManager;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import java.io.PrintWriter;
import java.net.URI;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class SamlEngineModule extends AbstractModule {

    private static final String REDIS_OBJECT_MAPPER = "RedisObjectMapper";
    public static final String COUNTRY_METADATA_HEALTH_CHECK = "CountryMetadataHealthCheck";
    public static final String EIDAS_HUB_ENTITY_ID_NOT_CONFIGURED_ERROR_MESSAGE = "eIDAS hub entity id is not configured";
    public static final String VERIFY_METADATA_RESOLVER = "VerifyMetadataResolver";
    public static final String FED_METADATA_ENTITY_SIGNATURE_VALIDATOR = "verifySignatureValidator";
    public static final String VERIFY_METADATA_SIGNATURE_TRUST_ENGINE = "VerifyMetadataSignatureTrustEngine";
    private HubTransformersFactory hubTransformersFactory = new HubTransformersFactory();

    @Override
    protected void configure() {
        bind(TrustStoreConfiguration.class).to(SamlEngineConfiguration.class);
        bind(RestfulClientConfiguration.class).to(SamlEngineConfiguration.class);
        bind(SamlDuplicateRequestValidationConfiguration.class).to(SamlEngineConfiguration.class);
        bind(SamlAuthnRequestValidityDurationConfiguration.class).to(SamlEngineConfiguration.class);
        bind(Client.class).toProvider(DefaultClientProvider.class).asEagerSingleton();
        bind(EntityToEncryptForLocator.class).to(AssignableEntityToEncryptForLocator.class);
        bind(AssignableEntityToEncryptForLocator.class).asEagerSingleton();
        bind(ReplayCacheStartupTasks.class).asEagerSingleton();
        bind(ConfigServiceKeyStore.class).asEagerSingleton();
        bind(JsonResponseProcessor.class);
        bind(RpErrorResponseGeneratorService.class);
        bind(TransactionsConfigProxy.class);
        bind(MatchingServiceHealthcheckRequestGeneratorService.class);
        bind(ExpiredCertificateMetadataFilter.class).toInstance(new ExpiredCertificateMetadataFilter());
        bind(new TypeLiteral<LevelLoggerFactory<SamlEngineExceptionMapper>>() {})
            .toInstance(new LevelLoggerFactory<>());
        bind(OutboundResponseFromHubToResponseTransformerFactory.class);
        bind(SimpleProfileOutboundResponseFromHubToResponseTransformerProvider.class);
        bind(SimpleProfileOutboundResponseFromHubToSamlResponseTransformer.class);
        bind(ResponseToUnsignedStringTransformer.class);
        bind(ResponseAssertionSigner.class);
        bind(SimpleProfileTransactionIdaStatusMarshaller.class);
        bind(EncryptedAssertionUnmarshaller.class).toInstance(hubTransformersFactory.getEncryptedAssertionUnmarshaller());
        bind(IdpAuthnResponseTranslatorService.class);
        bind(InboundResponseFromIdpDataGenerator.class);
        bind(MatchingServiceRequestGeneratorService.class);
        bind(HubAttributeQueryRequestBuilder.class);
        bind(MatchingServiceResponseTranslatorService.class);
        bind(RpAuthnRequestTranslatorService.class);
        bind(RpAuthnResponseGeneratorService.class);
        bind(IdpAuthnRequestGeneratorService.class);
        bind(IdaAuthnRequestTranslator.class);
        bind(EidasAuthnRequestTranslator.class);
        bind(MatchingServiceHealthcheckResponseTranslatorService.class);
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private ObjectMapper getObjectMapper(Environment environment) {
        return environment.getObjectMapper();
    }

    @Provides
    private HubEidasAttributeQueryRequestBuilder getHubEidasAttributeQueryRequestBuilder(@Named("HubEntityId") String  hubEntityId) {
        return new HubEidasAttributeQueryRequestBuilder(hubEntityId);
    }

    @Provides
    private Function<HubEidasAttributeQueryRequest, Element> getEidasMatchingServiceRequestElementTransformer(
        IdaKeyStore keyStore,
        EncryptionKeyStore encryptionKeyStore,
        EntityToEncryptForLocator entityToEncryptForLocator,
        SignatureAlgorithm signatureAlgorithm,
        DigestAlgorithm digestAlgorithm,
        @Named("HubEntityId") String hubEntityId
    ) {

        return hubTransformersFactory.getEidasMatchingServiceRequestToElementTransformer(
            keyStore,
            encryptionKeyStore,
            entityToEncryptForLocator,
            signatureAlgorithm,
            digestAlgorithm,
            hubEntityId);
    }

    @Provides
    private AttributeQueryGenerator<HubEidasAttributeQueryRequest> getHubEidasAttributeQueryRequestAttributeQueryGenerator(
        Function<HubEidasAttributeQueryRequest, Element> attributeQueryRequestTransformer,
        AssignableEntityToEncryptForLocator entityToEncryptForLocator) {
        return new AttributeQueryGenerator<>(attributeQueryRequestTransformer, entityToEncryptForLocator);
    }

    @Provides
    private CountryMatchingServiceRequestGeneratorService getCountryMatchingServiceRequestGeneratorService(
        HubEidasAttributeQueryRequestBuilder eidasAttributeQueryRequestBuilder,
        AttributeQueryGenerator<HubEidasAttributeQueryRequest> eidasAttributeQueryGenerator) {
        return new CountryMatchingServiceRequestGeneratorService(eidasAttributeQueryRequestBuilder, eidasAttributeQueryGenerator);
    }

    @Provides
    private CountryAuthnRequestGeneratorService getCountryAuthnRequestGeneratorService(Optional<CountrySingleSignOnServiceHelper> countrySingleSignOnServiceHelper,
                                                                                       Function<EidasAuthnRequestFromHub, String> eidasRequestStringTransformer,
                                                                                       EidasAuthnRequestTranslator eidasAuthnRequestTranslator,
                                                                                       @Named("HubEidasEntityId") Optional<String> hubEidasEntityId) {
        return new CountryAuthnRequestGeneratorService(
                countrySingleSignOnServiceHelper.orElseThrow(),
                eidasRequestStringTransformer,
                eidasAuthnRequestTranslator,
                hubEidasEntityId.orElseThrow(
                        () -> new InvalidConfigurationException(EIDAS_HUB_ENTITY_ID_NOT_CONFIGURED_ERROR_MESSAGE)));
    }

    @Provides
    private CountryAuthnResponseTranslatorService getCountryAuthnResponseTranslatorService(StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer,
                                                                                           ResponseFromCountryValidator responseFromCountryValidator,
                                                                                           @Named("ResponseAssertionsFromCountryValidator") Optional<ResponseAssertionsFromCountryValidator> responseAssertionFromCountryValidator,
                                                                                           Optional<DestinationValidator> validateSamlResponseIssuedByIdpDestination,
                                                                                           @Named("EidasAssertionDecrypter") AssertionDecrypter assertionDecrypter,
                                                                                           SecretKeyEncrypter secretKeyEncrypter,
                                                                                           AssertionBlobEncrypter assertionBlobEncrypter,
                                                                                           Optional<EidasValidatorFactory> eidasValidatorFactory,
                                                                                           PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller) {
        return new CountryAuthnResponseTranslatorService(
                stringToOpenSamlResponseTransformer,
                responseFromCountryValidator,
                responseAssertionFromCountryValidator.orElseThrow(() -> new InvalidConfigurationException("Eidas not configured correctly")),
                validateSamlResponseIssuedByIdpDestination.orElseThrow(() -> new InvalidConfigurationException("Eidas not configured correctly")),
                assertionDecrypter,
                secretKeyEncrypter,
                assertionBlobEncrypter,
                eidasValidatorFactory.orElseThrow(() -> new InvalidConfigurationException("Eidas not configured correctly")),
                passthroughAssertionUnmarshaller,
                new CountryAuthenticationStatusUnmarshaller());
    }

    @Provides
    public PassthroughAssertionUnmarshaller getPassthroughAssertionUnmarshaller() {
        return new PassthroughAssertionUnmarshaller(new XmlObjectToBase64EncodedStringTransformer<>(), new AuthnContextFactory());
    }

    @Provides
    public Optional<DestinationValidator> getValidateSamlResponseIssuedByIdpDestination(@Named("ExpectedEidasDestination") Optional<URI> expectedDestination) {
        return expectedDestination.map(d -> new DestinationValidator(d, Urls.FrontendUrls.SAML2_SSO_EIDAS_RESPONSE_ENDPOINT));
    }


    @Provides
    @Named("VERIFY_METADATA_REFRESH_TASK")
    @Singleton
    private Task registerMetadataRefreshTask(Environment environment, @Named(VERIFY_METADATA_RESOLVER) MetadataResolver metadataResolver) {
        Task task = new Task("metadata-refresh") {
            @Override
            public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
                ((AbstractReloadingMetadataResolver) metadataResolver).refresh();
            }
        };
        environment.admin().addTask(task);
        return task;
    }

    @Provides
    @Singleton
    private IdpSingleSignOnServiceHelper getIdpSingleSignOnServiceHelper(@Named("VerifyMetadataResolver") MetadataResolver metadataResolver) {
        return new IdpSingleSignOnServiceHelper(metadataResolver);
    }

    @Provides
    @Singleton
    private Optional<EidasMetadataResolverRepository> getCountryMetadataResolverRepository(Environment environment, SamlEngineConfiguration configuration){
        if (configuration.isEidasEnabled()) {
            EidasMetadataConfiguration eidasMetadataConfiguration = configuration.getCountryConfiguration().get().getMetadataConfiguration();
            URI trustAnchorUri = eidasMetadataConfiguration.getTrustAnchorUri();

            Client client = new ClientProvider(
                    environment,
                    eidasMetadataConfiguration.getJerseyClientConfiguration(),
                    true,
                    eidasMetadataConfiguration.getJerseyClientName()).get();

            KeyStore trustStore = eidasMetadataConfiguration.getTrustStore();

            EidasTrustAnchorResolver trustAnchorResolver = new EidasTrustAnchorResolver(
                    trustAnchorUri,
                    client,
                    trustStore);

            EidasMetadataResolverRepository eidasMetadataResolverRepository = new EidasMetadataResolverRepository(
                    trustAnchorResolver,
                    eidasMetadataConfiguration,
                    new DropwizardMetadataResolverFactory(),
                    new Timer(),
                    new MetadataSignatureTrustEngineFactory(),
                    new MetadataResolverConfigBuilder(),
                    client);

            registerEidasMetadataRefreshTask(environment, eidasMetadataResolverRepository, "eidas-metadata");
            return Optional.of(eidasMetadataResolverRepository);
        }
        return Optional.empty();
    }

    @Provides
    @Singleton
    public Optional<EidasValidatorFactory> getEidasValidatorFactory(Optional<EidasMetadataResolverRepository> eidasMetadataResolverRepository, SamlEngineConfiguration configuration){
        if (configuration.isEidasEnabled()){
            Optional<EidasValidatorFactory> eidasValidatorFactory = eidasMetadataResolverRepository
                    .map(EidasValidatorFactory::new);
            return eidasValidatorFactory;
        }
        return Optional.empty();
    }

    @Provides
    @Singleton
    public Optional<CountrySingleSignOnServiceHelper> getCountrySingleSignOnServiceHelper(Optional<EidasMetadataResolverRepository> eidasMetadataResolverRepository, SamlEngineConfiguration configuration){
        if (configuration.isEidasEnabled()){
            Optional<CountrySingleSignOnServiceHelper> countrySingleSignOnServiceHelper = eidasMetadataResolverRepository
                    .map(CountrySingleSignOnServiceHelper::new);
            return countrySingleSignOnServiceHelper;
        }
        return Optional.empty();
    }

    @Provides
    @Singleton
    public Optional<EidasTrustAnchorHealthCheck> getCountryMetadataHealthCheck(
        Optional<EidasMetadataResolverRepository> metadataResolverRepository,
        Environment environment){
        Optional<EidasTrustAnchorHealthCheck> metadataHealthCheck = metadataResolverRepository
                .map(EidasTrustAnchorHealthCheck::new);

        metadataHealthCheck.ifPresent(healthCheck -> environment.healthChecks().register(COUNTRY_METADATA_HEALTH_CHECK, healthCheck));

        return metadataHealthCheck;
    }

    @Provides
    @Singleton
    @Named("HubEntityId")
    private String getHubEntityId(SamlEngineConfiguration configuration) {
        return configuration.getSamlConfiguration().getEntityId();
    }

    @Provides
    @Singleton
    @Named("HubEidasEntityId")
    private Optional<String> getHubEidasEntityId(SamlEngineConfiguration configuration) {
        return configuration.getCountryConfiguration().map(c -> c.getSamlConfiguration().getEntityId());
    }

    @Provides
    @Singleton
    @Named("ExpectedEidasDestination")
    private Optional<URI> getExpectedEidasDestinationHost(SamlEngineConfiguration configuration) {
        return configuration.getCountryConfiguration().map(c -> c.getSamlConfiguration().getExpectedDestinationHost());
    }

    @Provides
    @Singleton
    private ResponseFromCountryValidator getResponseFromCountryValidator() {
        return new ResponseFromCountryValidator(new SamlStatusToCountryAuthenticationStatusCodeMapper());
    }

    @Provides
    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory() {
        return new OpenSamlXmlObjectFactory();
    }

    @Provides
    private XmlObjectToBase64EncodedStringTransformer<Response> responseXmlObjectToBase64EncodedStringTransformer() {
        return new XmlObjectToBase64EncodedStringTransformer<>();
    }

    @Provides
    private XmlObjectToBase64EncodedStringTransformer<Assertion> assertionXmlObjectToBase64EncodedStringTransformer() {
        return new XmlObjectToBase64EncodedStringTransformer<>();
    }

    @Provides
    @Singleton
    private JsonClient jsonClient(JsonResponseProcessor jsonResponseProcessor, Environment environment, SamlEngineConfiguration configuration) {
        Client client = new ClientProvider(
                environment,
                configuration.getJerseyClientConfiguration(),
                configuration.getEnableRetryTimeOutConnections(),
                "samlEngineClient").get();
        ErrorHandlingClient errorHandlingClient = new ErrorHandlingClient(client);
        return new JsonClient(errorHandlingClient, jsonResponseProcessor);
    }

    @Provides
    @Singleton
    private IdaKeyStore getKeyStore(SamlEngineConfiguration configuration, SigningCertFromMetadataExtractor signingCertExtractor) {
        try {
            PrivateKey primaryEncryptionKey = configuration.getPrimaryPrivateEncryptionKeyConfiguration().getPrivateKey();
            PrivateKey secondaryEncryptionKey = configuration.getSecondaryPrivateEncryptionKeyConfiguration().getPrivateKey();
            PrivateKey signingKey = configuration.getPrivateSigningKeyConfiguration().getPrivateKey();
            PublicKey publicSigningKey = KeySupport.derivePublicKey(signingKey);

            KeyPair primaryEncryptionKeyPair = new KeyPair(KeySupport.derivePublicKey(primaryEncryptionKey), primaryEncryptionKey);
            KeyPair secondaryEncryptionKeyPair = new KeyPair(KeySupport.derivePublicKey(secondaryEncryptionKey), secondaryEncryptionKey);
            KeyPair signingKeyPair = new KeyPair(publicSigningKey, signingKey);

            X509Certificate signingCertificate = signingCertExtractor.getSigningCertForCurrentSigningKey(publicSigningKey);

            return new IdaKeyStore(signingCertificate, signingKeyPair, asList(primaryEncryptionKeyPair, secondaryEncryptionKeyPair));
        } catch (KeyException e) {
            throw new KeyLoadingException(e);
        }
    }

    @Provides
    @Singleton
    private SamlConfiguration samlConfiguration(SamlEngineConfiguration configuration) {
        return configuration.getSamlConfiguration();
    }

    @Provides
    @Singleton
    private ServiceInfoConfiguration serviceInfoConfiguration(SamlEngineConfiguration configuration) {
        return configuration.getServiceInfo();
    }

    @Provides
    @Singleton
    @Config
    private URI configUri(SamlEngineConfiguration configurations) {
        return configurations.getConfigUri();
    }

    @Provides
    @SuppressWarnings("unused")
    private OutboundLegacyResponseFromHubToStringFunction getOutboundLegacyResponseFromHubToSignedResponseTransformerProvider(
            EncryptionKeyStore encryptionKeyStore,
            IdaKeyStore keyStore,
            EntityToEncryptForLocator entityToEncryptForLocator,
            ResponseAssertionSigner responseAssertionSigner,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm) {

        return new OutboundLegacyResponseFromHubToStringFunction(
                hubTransformersFactory.getOutboundResponseFromHubToStringTransformer(
                        encryptionKeyStore,
                        keyStore,
                        entityToEncryptForLocator,
                        responseAssertionSigner,
                        signatureAlgorithm,
                        digestAlgorithm
                )
        );
    }

    @Provides
    @SuppressWarnings("unused")
    private OutboundLegacyResponseFromHubToStringFunctionSHA256 getOutboundLegacyResponseFromHubToSignedResponseTransformerProvider(
            EncryptionKeyStore encryptionKeyStore,
            IdaKeyStore keyStore,
            EntityToEncryptForLocator entityToEncryptForLocator,
            ResponseAssertionSigner responseAssertionSigner,
            DigestAlgorithm digestAlgorithm) {

        return new OutboundLegacyResponseFromHubToStringFunctionSHA256(
                hubTransformersFactory.getOutboundResponseFromHubToStringTransformer(
                        encryptionKeyStore,
                        keyStore,
                        entityToEncryptForLocator,
                        responseAssertionSigner,
                        new SignatureRSASHA256(),
                        digestAlgorithm
                )
        );
    }

    @Provides
    @SuppressWarnings("unused")
    private OutboundSamlProfileResponseFromHubToStringFunction getOutboundSamlProfileResponseFromHubToSignedResponseTransformerProvider(
            EncryptionKeyStore encryptionKeyStore,
            IdaKeyStore keyStore,
            EntityToEncryptForLocator entityToEncryptForLocator,
            ResponseAssertionSigner responseAssertionSigner,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm) {

        return new OutboundSamlProfileResponseFromHubToStringFunction(
                hubTransformersFactory.getSamlProfileOutboundResponseFromHubToStringTransformer(
                        encryptionKeyStore,
                        keyStore,
                        entityToEncryptForLocator,
                        responseAssertionSigner,
                        signatureAlgorithm,
                        digestAlgorithm
                )
        );
    }

    @Provides
    @SuppressWarnings("unused")
    private OutboundSamlProfileResponseFromHubToStringFunctionSHA256 getOutboundSamlProfileResponseFromHubToSignedResponseTransformerProviderSHA256(
            EncryptionKeyStore encryptionKeyStore,
            IdaKeyStore keyStore,
            EntityToEncryptForLocator entityToEncryptForLocator,
            ResponseAssertionSigner responseAssertionSigner,
            DigestAlgorithm digestAlgorithm) {

        return new OutboundSamlProfileResponseFromHubToStringFunctionSHA256(
                hubTransformersFactory.getSamlProfileOutboundResponseFromHubToStringTransformer(
                        encryptionKeyStore,
                        keyStore,
                        entityToEncryptForLocator,
                        responseAssertionSigner,
                        new SignatureRSASHA256(),
                        digestAlgorithm
                )
        );
    }

    @Provides
    @Singleton
    @Named(REDIS_OBJECT_MAPPER)
    private ObjectMapper getRedisObjectMapper() {
        return new ObjectMapper()
                .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new JodaModule());
    }

    @Provides
    @Singleton
    private IdExpirationCache<String> assertionIdCache(SamlEngineConfiguration configuration,
                                                       @Named(REDIS_OBJECT_MAPPER) ObjectMapper objectMapper,
                                                       InfinispanCacheManager infinispanCacheManager) {
        RedisCodec<String, DateTime> codec = new AssertionExpirationCacheRedisCodec(objectMapper);
        return configuration.getRedis()
                .map(rc -> this.getIdExpirationCache(rc, codec, 1))
                .orElseGet(() -> new ConcurrentMapIdExpirationCache<>(infinispanCacheManager.getCache("assertion_id_cache")));
    }

    @Provides
    @Singleton
    private IdExpirationCache<AuthnRequestIdKey> authRequestIdCache(SamlEngineConfiguration configuration,
                                                                    @Named(REDIS_OBJECT_MAPPER) ObjectMapper objectMapper,
                                                                    InfinispanCacheManager infinispanCacheManager) {
        RedisCodec<AuthnRequestIdKey, DateTime> codec = new AuthnRequestExpirationCacheRedisCodec(objectMapper);
        return configuration.getRedis()
                .map(rc -> this.getIdExpirationCache(rc, codec, 0))
                .orElseGet(() -> new ConcurrentMapIdExpirationCache<>(infinispanCacheManager.getCache("authn_request_id_cache")));
    }

    private <T> IdExpirationCache<T> getIdExpirationCache(RedisConfiguration config,
                                                          RedisCodec<T, DateTime> codec,
                                                          int dbIndex) {
        RedisClient redisClient = RedisClient.create();
        RedisURI uri = config.getUri();
        uri.setDatabase(dbIndex);

        StatefulRedisMasterSlaveConnection<T, DateTime> redisConnection = MasterSlave.connect(
                redisClient,
                codec,
                singletonList(uri)
        );

        RedisCommands<T, DateTime> redisCommands = redisConnection.sync();
        return new RedisIdExpirationCache<>(redisCommands, config.getRecordTTL());
    }

    @Provides
    @SuppressWarnings("unused")
    private AssertionFromIdpToAssertionTransformer getAssertionFromIdpToAssertionTransformer() {
        return hubTransformersFactory.getAssertionFromIdpToAssertionTransformer();
    }

    @Provides
    private StringToOpenSamlObjectTransformer<AuthnRequest> getStringAuthnRequestTransformer() {
        return hubTransformersFactory.getStringToAuthnRequestTransformer();
    }

    @Provides
    private AuthnRequestToIdaRequestFromRelyingPartyTransformer getAuthnRequestAuthnRequestFromRelyingPartyTransformer(
            @Named("authnRequestKeyStore") SigningKeyStore signingKeyStore,
            IdaKeyStore decryptionKeyStore,
            SamlConfiguration samlConfiguration,
            IdExpirationCache<AuthnRequestIdKey> duplicateIds,
            SamlDuplicateRequestValidationConfiguration duplicateRequestValidationConfiguration,
            SamlAuthnRequestValidityDurationConfiguration authnRequestValidityDurationConfiguration
    ) {
        return hubTransformersFactory.getAuthnRequestToAuthnRequestFromTransactionTransformer(
                samlConfiguration.getExpectedDestinationHost(),
                signingKeyStore,
                decryptionKeyStore,
                duplicateIds,
                duplicateRequestValidationConfiguration,
                authnRequestValidityDurationConfiguration
        );
    }

    @Provides
    private AttributeQueryGenerator<MatchingServiceHealthCheckRequest> matchingServiceHealthCheckRequestAttributeQueryGenerator(Function<MatchingServiceHealthCheckRequest, Element> attributeQueryRequestTransformer, AssignableEntityToEncryptForLocator entityToEncryptForLocator) {
        return new AttributeQueryGenerator<>(attributeQueryRequestTransformer, entityToEncryptForLocator);
    }

    @Provides
    private AttributeQueryGenerator<HubAttributeQueryRequest> hubAttributeQueryRequestAttributeQueryGenerator(Function<HubAttributeQueryRequest, Element> attributeQueryRequestTransformer, AssignableEntityToEncryptForLocator entityToEncryptForLocator) {
        return new AttributeQueryGenerator<>(attributeQueryRequestTransformer, entityToEncryptForLocator);
    }

    @Provides
    private StringToOpenSamlObjectTransformer<Response> getStringIdaResponseIssuedByIdpTransformer() {
        return hubTransformersFactory.getStringToResponseTransformer();
    }

    @Provides
    private StringToOpenSamlObjectTransformer<Assertion> getStringToAssertionTransformer() {
        return hubTransformersFactory.getStringToAssertionTransformer();
    }

    @Provides
    @Named(FED_METADATA_ENTITY_SIGNATURE_VALIDATOR)
    private MetadataBackedSignatureValidator fedMetadataEntitySignatureValidator(@Named(VERIFY_METADATA_SIGNATURE_TRUST_ENGINE) ExplicitKeySignatureTrustEngine explicitKeySignatureTrustEngine) {
       return MetadataBackedSignatureValidator.withoutCertificateChainValidation(explicitKeySignatureTrustEngine);
    }

    @Provides
    @Named("IdpSamlResponseTransformer")
    private DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer getResponseToInboundResponseFromIdpTransformer(
            IdExpirationCache<String> assertionIdCache,
            SamlConfiguration samlConfiguration,
            @Named(FED_METADATA_ENTITY_SIGNATURE_VALIDATOR) MetadataBackedSignatureValidator idpSignatureValidator,
            IdaKeyStore keyStore,
            @Named("HubEntityId") String hubEntityId) {

        return hubTransformersFactory.getDecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(
                idpSignatureValidator, keyStore, samlConfiguration.getExpectedDestinationHost(),
                Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT, assertionIdCache,
                hubEntityId);
    }

    @Provides
    @Singleton
    private SigningCertFromMetadataExtractor getSigningCertFromMetadataExtractor(@Named(VERIFY_METADATA_RESOLVER) MetadataResolver metadataResolver, @Named("HubEntityId") String hubEntityId) throws ComponentInitializationException {
        return new SigningCertFromMetadataExtractor(metadataResolver, hubEntityId);
    }

    @Provides
    @Named("EidasAssertionDecrypter")
    private AssertionDecrypter getEidasAssertionDecrypter(IdaKeyStoreCredentialRetriever idaKeyStoreCredentialRetriever) {
        Decrypter decrypter = new DecrypterFactory().createDecrypter(idaKeyStoreCredentialRetriever.getDecryptingCredentials());
        Set<String> contentEncryptionAlgorithms = Set.of(
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM);
        Set<String> keyTransportAlgorithms = Set.of(
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11);
        return new AssertionDecrypter(
            new EncryptionAlgorithmValidator(contentEncryptionAlgorithms, keyTransportAlgorithms),
            decrypter
        );
    }

    @Provides
    private SecretKeyEncrypter getSecretKeyEncrypter(KeyStoreBackedEncryptionCredentialResolver keyStoreBackedEncryptionCredentialResolver) {
        return new SecretKeyEncrypter(keyStoreBackedEncryptionCredentialResolver);
    }
    @Provides
    @Singleton
    @Named("ResponseAssertionsFromCountryValidator")
    private Optional<ResponseAssertionsFromCountryValidator> getResponseAssertionsFromCountryValidator(
            final IdExpirationCache<String> assertionIdCache,
            @Named("ExpectedEidasDestination") Optional<URI>  expectedDestination) {
        return  expectedDestination.map(destination -> new ResponseAssertionsFromCountryValidator(
                new IdentityProviderAssertionValidator(
                        new IssuerValidator(),
                        new AssertionSubjectValidator(),
                        new AssertionAttributeStatementValidator(),
                        new AssertionSubjectConfirmationValidator()
                ),
                new EidasAttributeStatementAssertionValidator(),
                new AuthnStatementAssertionValidator(
                        new DuplicateAssertionValidatorImpl(assertionIdCache)
                ),
                new EidasAuthnResponseIssuerValidator(),
                destination.toString())
        );
    }

    @Provides
    private ElementToOpenSamlXMLObjectTransformer<Response> getElementInboundResponseFromMatchingServiceTransformer() {
        return new CoreTransformersFactory()
                .getElementToOpenSamlXmlObjectTransformer();
    }

    @Provides
    private DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer getResponseToInboundResponseFromMatchingServiceTransformer(
            @Named("samlResponseFromMatchingServiceKeyStore") SigningKeyStore publicKeyStore,
            IdaKeyStore keyStore,
            @Named("HubEntityId") String hubEntityId) {
        return hubTransformersFactory.getResponseToInboundResponseFromMatchingServiceTransformer(
                publicKeyStore,
                keyStore,
                hubEntityId
        );
    }

    @Provides
    @SuppressWarnings("unused")
    private Function<HubAttributeQueryRequest, Element> getMatchingServiceRequestElementTransformer(
            IdaKeyStore keyStore,
            EncryptionKeyStore encryptionKeyStore,
            EntityToEncryptForLocator entityToEncryptForLocator,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm,
            @Named("HubEntityId") String hubEntityId
    ) {
        return hubTransformersFactory.getMatchingServiceRequestToElementTransformer(
                keyStore,
                encryptionKeyStore,
                entityToEncryptForLocator,
                signatureAlgorithm,
                digestAlgorithm,
                hubEntityId
        );
    }

    @Provides
    @SuppressWarnings("unused")
    private Function<IdaAuthnRequestFromHub, String> getIdaAuthnRequestFromHubStringTransformer(
            IdaKeyStore keyStore,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm) {
        return hubTransformersFactory.getIdaAuthnRequestFromHubToStringTransformer(
                keyStore,
                signatureAlgorithm,
                digestAlgorithm
        );
    }

    @Provides
    @SuppressWarnings("unused")
    private Function<EidasAuthnRequestFromHub, String> getEidasAuthnRequestFromHubStringTransformer(
            IdaKeyStore keyStore,
            DigestAlgorithm digestAlgorithm) {
        return hubTransformersFactory.getEidasAuthnRequestFromHubToStringTransformer(
                keyStore,
                new SignatureRSASHA256(),
                digestAlgorithm
        );
    }

    @Provides
    private DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer getResponseInboundHealthCheckResponseFromMatchingServiceTransformer(
            @Named("samlResponseFromMatchingServiceKeyStore") SigningKeyStore authnResponseKeyStore) {
        return hubTransformersFactory.getResponseInboundHealthCheckResponseFromMatchingServiceTransformer(authnResponseKeyStore);
    }

    @Provides
    @SuppressWarnings("unused")
    private Function<MatchingServiceHealthCheckRequest, Element> getMatchingServiceHealthCheckRequestElementTransformer(
            IdaKeyStore keyStore,
            EncryptionKeyStore encryptionKeyStore,
            EntityToEncryptForLocator entityToEncryptForLocator,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm,
            @Named("HubEntityId") String hubEntityId
    ) {
        return hubTransformersFactory.getMatchingServiceHealthCheckRequestToElementTransformer(
                keyStore,
                encryptionKeyStore,
                entityToEncryptForLocator,
                signatureAlgorithm,
                digestAlgorithm,
                hubEntityId
        );
    }

    @Provides
    private AssertionEncrypter assertionEncrypter(KeyStoreBackedEncryptionCredentialResolver credentialResolver) {
        return new AssertionEncrypter(new EncrypterFactory(), credentialResolver);
    }

    @Provides
    @Singleton
    private IdpAssertionMetricsCollector metricsCollector(Environment environment) {
        return new IdpAssertionMetricsCollector(environment.metrics());
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
