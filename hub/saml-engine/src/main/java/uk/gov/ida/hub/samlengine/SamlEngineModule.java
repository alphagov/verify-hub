package uk.gov.ida.hub.samlengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import io.dropwizard.setup.Environment;
import org.joda.time.DateTime;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.w3c.dom.Element;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.shared.configuration.DeserializablePublicKeyConfiguration;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlengine.annotations.Config;
import uk.gov.ida.hub.samlengine.attributequery.AttributeQueryGenerator;
import uk.gov.ida.hub.samlengine.attributequery.HubAttributeQueryRequestBuilder;
import uk.gov.ida.hub.samlengine.attributequery.HubEidasAttributeQueryRequestBuilder;
import uk.gov.ida.hub.samlengine.config.ConfigServiceKeyStore;
import uk.gov.ida.hub.samlengine.config.SamlConfiguration;
import uk.gov.ida.hub.samlengine.exceptions.InvalidConfigurationException;
import uk.gov.ida.hub.samlengine.exceptions.SamlEngineExceptionMapper;
import uk.gov.ida.hub.samlengine.factories.OutboundResponseFromHubToResponseTransformerFactory;
import uk.gov.ida.hub.samlengine.locators.AssignableEntityToEncryptForLocator;
import uk.gov.ida.hub.samlengine.logging.IdpAssertionMetricsCollector;
import uk.gov.ida.hub.samlengine.proxy.CountrySingleSignOnServiceHelper;
import uk.gov.ida.hub.samlengine.proxy.IdpSingleSignOnServiceHelper;
import uk.gov.ida.hub.samlengine.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.samlengine.security.AuthnResponseKeyStore;
import uk.gov.ida.hub.samlengine.security.Crypto;
import uk.gov.ida.hub.samlengine.security.PrivateKeyFileDescriptors;
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
import uk.gov.ida.saml.core.transformers.outbound.decorators.NullResponseAssertionSigner;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseAssertionSigner;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.core.validators.assertion.AssertionAttributeStatementValidator;
import uk.gov.ida.saml.core.validators.assertion.AuthnStatementAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.DuplicateAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.core.validators.subject.AssertionSubjectValidator;
import uk.gov.ida.saml.core.validators.subjectconfirmation.AssertionSubjectConfirmationValidator;
import uk.gov.ida.saml.deserializers.ElementToOpenSamlXMLObjectTransformer;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.api.HubTransformersFactory;
import uk.gov.ida.saml.hub.configuration.SamlAuthnRequestValidityDurationConfiguration;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.HubAttributeQueryRequest;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;
import uk.gov.ida.saml.hub.domain.IdaAuthnRequestFromHub;
import uk.gov.ida.saml.hub.domain.IdpIdaStatus;
import uk.gov.ida.saml.hub.domain.MatchingServiceHealthCheckRequest;
import uk.gov.ida.saml.hub.transformers.inbound.AuthnRequestToIdaRequestFromRelyingPartyTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.IdpIdaStatusUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.InboundResponseFromIdpDataGenerator;
import uk.gov.ida.saml.hub.transformers.inbound.PassthroughAssertionUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdpIdaStatusMappingsFactory;
import uk.gov.ida.saml.hub.transformers.inbound.decorators.ValidateSamlResponseIssuedByIdpDestination;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.AssertionFromIdpToAssertionTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundLegacyResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundSamlProfileResponseFromHubToStringFunction;
import uk.gov.ida.saml.hub.transformers.outbound.SimpleProfileOutboundResponseFromHubToSamlResponseTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.SimpleProfileTransactionIdaStatusMarshaller;
import uk.gov.ida.saml.hub.transformers.outbound.providers.ResponseToUnsignedStringTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.providers.SimpleProfileOutboundResponseFromHubToResponseTransformerProvider;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIdKey;
import uk.gov.ida.saml.metadata.ExpiredCertificateMetadataFilter;
import uk.gov.ida.saml.metadata.IdpMetadataPublicKeyStore;
import uk.gov.ida.saml.metadata.MetadataHealthCheck;
import uk.gov.ida.saml.metadata.factories.DropwizardMetadataResolverFactory;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.CredentialFactorySignatureValidator;
import uk.gov.ida.saml.security.DecrypterFactory;
import uk.gov.ida.saml.security.EncrypterFactory;
import uk.gov.ida.saml.security.EncryptionCredentialFactory;
import uk.gov.ida.saml.security.EncryptionKeyStore;
import uk.gov.ida.saml.security.EntityToEncryptForLocator;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.saml.security.IdaKeyStoreCredentialRetriever;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.saml.security.SigningCredentialFactory;
import uk.gov.ida.saml.security.SigningKeyStore;
import uk.gov.ida.saml.security.validators.encryptedelementtype.EncryptionAlgorithmValidator;
import uk.gov.ida.saml.security.validators.issuer.IssuerValidator;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;
import uk.gov.ida.shared.dropwizard.infinispan.util.InfinispanCacheManager;
import uk.gov.ida.shared.utils.logging.LevelLoggerFactory;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static java.util.Arrays.asList;

public class SamlEngineModule extends AbstractModule {

    public static final String VERIFY_METADATA_HEALTH_CHECK = "VerifyMetadataHealthCheck";
    public static final String COUNTRY_METADATA_HEALTH_CHECK = "CountryMetadataHealthCheck";
    public static final String EIDAS_HUB_ENTITY_ID_NOT_CONFIGURED_ERROR_MESSAGE = "eIDAS hub entity id is not configured";
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
        bind(InfinispanStartupTasks.class).asEagerSingleton();
        bind(ConfigServiceKeyStore.class).asEagerSingleton();
        bind(JsonResponseProcessor.class);
        bind(RpErrorResponseGeneratorService.class);
        bind(TransactionsConfigProxy.class);
        bind(MatchingServiceHealthcheckRequestGeneratorService.class);
        bind(ObjectMapper.class).toInstance(new ObjectMapper());
        bind(ExpiredCertificateMetadataFilter.class).toInstance(new ExpiredCertificateMetadataFilter());
        bind(new TypeLiteral<LevelLoggerFactory<SamlEngineExceptionMapper>>() {})
            .toInstance(new LevelLoggerFactory<>());
        bind(OutboundResponseFromHubToResponseTransformerFactory.class);
        bind(SimpleProfileOutboundResponseFromHubToResponseTransformerProvider.class);
        bind(SimpleProfileOutboundResponseFromHubToSamlResponseTransformer.class);
        bind(ResponseToUnsignedStringTransformer.class);
        bind(ResponseAssertionSigner.class).toInstance(new NullResponseAssertionSigner());
        bind(SimpleProfileTransactionIdaStatusMarshaller.class);
        bind(IdpAuthnResponseTranslatorService.class);
        bind(InboundResponseFromIdpDataGenerator.class);
        bind(MatchingServiceRequestGeneratorService.class);
        bind(HubAttributeQueryRequestBuilder.class);
        bind(MatchingServiceResponseTranslatorService.class);
        bind(RpAuthnRequestTranslatorService.class);
        bind(RpAuthnResponseGeneratorService.class);
        bind(IdpAuthnRequestGeneratorService.class);
        bind(CountrySingleSignOnServiceHelper.class);
        bind(IdaAuthnRequestTranslator.class);
        bind(EidasAuthnRequestTranslator.class);
        bind(MatchingServiceHealthcheckResponseTranslatorService.class);
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
        @Named("HubEidasEntityId") Optional<String> hubEidasEntityId
    ) {
        if (!hubEidasEntityId.isPresent()) {
            throw new InvalidConfigurationException(EIDAS_HUB_ENTITY_ID_NOT_CONFIGURED_ERROR_MESSAGE);
        }
        return hubTransformersFactory.getEidasMatchingServiceRequestToElementTransformer(
            keyStore,
            encryptionKeyStore,
            entityToEncryptForLocator,
            signatureAlgorithm,
            digestAlgorithm,
            hubEidasEntityId.get());
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
    private CountryAuthnRequestGeneratorService getCountryAuthnRequestGeneratorService(CountrySingleSignOnServiceHelper countrySingleSignOnServiceHelper,
                                                                                       Function<EidasAuthnRequestFromHub, String> eidasRequestStringTransformer,
                                                                                       EidasAuthnRequestTranslator eidasAuthnRequestTranslator,
                                                                                       @Named("HubEidasEntityId") Optional<String> hubEidasEntityId) {
        if (!hubEidasEntityId.isPresent()) {
            throw new InvalidConfigurationException(EIDAS_HUB_ENTITY_ID_NOT_CONFIGURED_ERROR_MESSAGE);
        }
        return new CountryAuthnRequestGeneratorService(countrySingleSignOnServiceHelper, eidasRequestStringTransformer, eidasAuthnRequestTranslator, hubEidasEntityId.get());
    }

    @Provides
    private CountryAuthnResponseTranslatorService getCountryAuthnResponseTranslatorService(StringToOpenSamlObjectTransformer<Response> stringToOpenSamlResponseTransformer,
                                                                                           ResponseFromCountryValidator responseFromCountryValidator,
                                                                                           IdpIdaStatusUnmarshaller idpIdaStatusUnmarshaller,
                                                                                           @Named("ResponseAssertionsFromCountryValidator") Optional<ResponseAssertionsFromCountryValidator> responseAssertionFromCountryValidator,
                                                                                           Optional<ValidateSamlResponseIssuedByIdpDestination> validateSamlResponseIssuedByIdpDestination,
                                                                                           @Named("AES256DecrypterWithGCM") AssertionDecrypter assertionDecrypter,
                                                                                           AssertionBlobEncrypter assertionBlobEncrypter,
                                                                                           @Named("CountrySamlResponseSignatureValidator") Optional<SamlResponseSignatureValidator> responseSignatureValidator,
                                                                                           @Named("CountrySamlAssertionsSignatureValidator") Optional<SamlAssertionsSignatureValidator> assertionSignatureValidator,
                                                                                           PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller) {
        if (!responseAssertionFromCountryValidator.isPresent() || !validateSamlResponseIssuedByIdpDestination.isPresent() || !responseSignatureValidator.isPresent() || !assertionSignatureValidator.isPresent()) {
            throw new InvalidConfigurationException("Eidas not configured correctly");
        }
        return new CountryAuthnResponseTranslatorService(stringToOpenSamlResponseTransformer,
                responseFromCountryValidator,
                idpIdaStatusUnmarshaller,
                responseAssertionFromCountryValidator.get(),
                validateSamlResponseIssuedByIdpDestination.get(),
                assertionDecrypter,
                assertionBlobEncrypter,
                responseSignatureValidator.get(),
                assertionSignatureValidator.get(),
                passthroughAssertionUnmarshaller);
    }

    @Provides
    public PassthroughAssertionUnmarshaller getPassthroughAssertionUnmarshaller() {
        return new PassthroughAssertionUnmarshaller(new XmlObjectToBase64EncodedStringTransformer<>(), new AuthnContextFactory());
    }

    @Provides
    public Optional<ValidateSamlResponseIssuedByIdpDestination> getValidateSamlResponseIssuedByIdpDestination(@Named("ExpectedEidasDestination") Optional<URI> expectedDestination) {
        return expectedDestination.map(d -> new ValidateSamlResponseIssuedByIdpDestination(
                new DestinationValidator(d), Urls.FrontendUrls.SAML2_SSO_EIDAS_RESPONSE_ENDPOINT));
    }

    @Provides
    public IdpIdaStatusUnmarshaller getIdpIdaStatusUnmarshaller() {
        return new IdpIdaStatusUnmarshaller(new IdpIdaStatus.IdpIdaStatusFactory(), new SamlStatusToIdpIdaStatusMappingsFactory());
    }

    @Provides
    @Singleton
    @Named("VerifyMetadataResolver")
    private MetadataResolver getVerifyMetadataResolver(Environment environment, SamlEngineConfiguration configuration) {
        return new DropwizardMetadataResolverFactory().createMetadataResolver(environment, configuration.getMetadataConfiguration());
    }

    @Provides
    @Singleton
    @Named(VERIFY_METADATA_HEALTH_CHECK)
    private MetadataHealthCheck getVerifyMetadataHealthCheck(
        @Named("VerifyMetadataResolver") MetadataResolver metadataResolver,
        Environment environment,
        SamlEngineConfiguration configuration) {
        MetadataHealthCheck metadataHealthCheck = new MetadataHealthCheck(metadataResolver, configuration.getMetadataConfiguration().getExpectedEntityId());
        environment.healthChecks().register(VERIFY_METADATA_HEALTH_CHECK, metadataHealthCheck);
        return metadataHealthCheck;
    }

    @Provides
    @Singleton
    private IdpSingleSignOnServiceHelper getIdpSingleSignOnServiceHelper(@Named("VerifyMetadataResolver") MetadataResolver metadataResolver) {
        return new IdpSingleSignOnServiceHelper(metadataResolver);
    }

    @Provides
    @Singleton
    @Named("IdpMetadataPublicKeyStore")
    private IdpMetadataPublicKeyStore getIdpMetadataPublicKeyStore(@Named("VerifyMetadataResolver") MetadataResolver metadataResolver) {
        return new IdpMetadataPublicKeyStore(metadataResolver);
    }

    @Provides
    @Named("authnResponseKeyStore")
    public SigningKeyStore getCountryAuthnResponseKeyStore(@Named("IdpMetadataPublicKeyStore") IdpMetadataPublicKeyStore idpMetadataPublicKeyStore) {
        return new AuthnResponseKeyStore(idpMetadataPublicKeyStore);
    }

    @Provides
    @Singleton
    @Named("CountryMetadataResolver")
    private Optional<MetadataResolver> getCountryMetadataResolver(Environment environment, SamlEngineConfiguration configuration) {
        return configuration.getCountryConfiguration().map(config -> new DropwizardMetadataResolverFactory().createMetadataResolver(environment, config.getMetadataConfiguration()));
    }

    @Provides
    @Singleton
    @Named(COUNTRY_METADATA_HEALTH_CHECK)
    public Optional<MetadataHealthCheck> getCountryMetadataHealthCheck(
        @Named("CountryMetadataResolver") Optional<MetadataResolver> metadataResolver,
        Environment environment,
        SamlEngineConfiguration configuration) {
        Optional<MetadataHealthCheck> metadataHealthCheck = metadataResolver.map(resolver -> new MetadataHealthCheck(resolver, configuration.getCountryConfiguration().get().getMetadataConfiguration().getExpectedEntityId()));

        return metadataHealthCheck.map(healthCheck -> {
            environment.healthChecks().register(COUNTRY_METADATA_HEALTH_CHECK, healthCheck);
            return healthCheck;
        });
    }

    @Provides
    @Singleton
    @Named("CountryMetadataPublicKeyStore")
    private Optional<IdpMetadataPublicKeyStore> getCountryMetadataPublicKeyStore(@Named("CountryMetadataResolver") Optional<MetadataResolver> metadataResolver) {
        return metadataResolver.map(IdpMetadataPublicKeyStore::new);
    }

    @Provides
    @Singleton
    @Named("countryAuthnResponseKeyStore")
    public Optional<SigningKeyStore> getCountryAuthnResponseKeyStore(@Named("CountryMetadataPublicKeyStore") Optional<IdpMetadataPublicKeyStore> countryMetadataPublicKeyStore) {
        return countryMetadataPublicKeyStore.map(AuthnResponseKeyStore::new);
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
        return new ResponseFromCountryValidator(new SamlStatusToIdpIdaStatusMappingsFactory());
    }

    @Provides
    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory() {
        return new OpenSamlXmlObjectFactory();
    }

    @Provides
    private XmlObjectToBase64EncodedStringTransformer<Response> responseXmlObjectToBase64EncodedStringTransformer() {
        return new XmlObjectToBase64EncodedStringTransformer<Response>();
    }

    @Provides
    private XmlObjectToBase64EncodedStringTransformer<Assertion> assertionXmlObjectToBase64EncodedStringTransformer() {
        return new XmlObjectToBase64EncodedStringTransformer<Assertion>();
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
    private IdaKeyStore getKeyStore(X509CertificateFactory certificateFactory, SamlEngineConfiguration configuration) {
        Map<KeyPosition, PrivateKey> privateKeyStore = privateEncryptionKeys(configuration);

        DeserializablePublicKeyConfiguration publicSigningKeyConfiguration = configuration.getPublicSigningCert();
        String encodedSigningCertificate = publicSigningKeyConfiguration.getCert();
        X509Certificate signingCertificate = encodedSigningCertificate != null ? certificateFactory.createCertificate(encodedSigningCertificate) : null;

        KeyPair primaryEncryptionKeyPair = Crypto.keyPairFromPrivateKey(privateKeyStore.get(KeyPosition.PRIMARY));
        KeyPair secondaryEncryptionKeyPair = Crypto.keyPairFromPrivateKey(privateKeyStore.get(KeyPosition.SECONDARY));
        KeyPair signingKeyPair = Crypto.keyPairFromPrivateKey(privateSigningKey(configuration));

        return new IdaKeyStore(signingCertificate, signingKeyPair, asList(primaryEncryptionKeyPair, secondaryEncryptionKeyPair));
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
    private ClientTrustStoreConfiguration clientTrustStoreConfiguration(SamlEngineConfiguration configuration) {
        return configuration.getClientTrustStoreConfiguration();
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
    @Singleton
    private ConcurrentMap<String, DateTime> assertionIdCache(InfinispanCacheManager infinispanCacheManager) {
        return infinispanCacheManager.<String, DateTime>getCache("assertion_id_cache");
    }

    @Provides
    @Singleton
    private ConcurrentMap<AuthnRequestIdKey, DateTime> authRequestIdCache(InfinispanCacheManager infinispanCacheManager) {
        return infinispanCacheManager.<AuthnRequestIdKey, DateTime>getCache("authn_request_id_cache");
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
            ConcurrentMap<AuthnRequestIdKey, DateTime> duplicateIds,
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
    @Named("IdpSamlResponseTransformer")
    private DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer getResponseToInboundResponseFromIdpTransformer(
            ConcurrentMap<String, DateTime> assertionIdCache,
            SamlConfiguration samlConfiguration,
            @Named("authnResponseKeyStore") SigningKeyStore authnResponseKeyStore,
            IdaKeyStore keyStore,
            @Named("HubEntityId") String hubEntityId) {

        return hubTransformersFactory.getDecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(
                authnResponseKeyStore, keyStore, samlConfiguration.getExpectedDestinationHost(),
                Urls.FrontendUrls.SAML2_SSO_RESPONSE_ENDPOINT, assertionIdCache,
                hubEntityId);
    }

    @Provides
    @Named("AES256DecrypterWithGCM")
    private AssertionDecrypter getAES256WithGCMAssertionDecrypter(IdaKeyStore keyStore) {
        return new AssertionDecrypter(
                new IdaKeyStoreCredentialRetriever(keyStore), new EncryptionAlgorithmValidator(ImmutableSet.of(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256, EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM)), new DecrypterFactory()
        );
    }

    @Provides
    @Singleton
    @Named("CountrySamlMessageSignatureValidator")
    private Optional<SamlMessageSignatureValidator> getCountrySamlMessageSignatureValidator(@Named("countryAuthnResponseKeyStore") Optional<SigningKeyStore> countrySigningKeyStore) {
        return countrySigningKeyStore.map(store -> new SamlMessageSignatureValidator(new CredentialFactorySignatureValidator(new SigningCredentialFactory(store))));
    }

    @Provides
    @Singleton
    @Named("CountrySamlResponseSignatureValidator")
    private Optional<SamlResponseSignatureValidator> getCountrySamlResponseSignatureValidator(@Named("CountrySamlMessageSignatureValidator") Optional<SamlMessageSignatureValidator> samlMessageSignatureValidator) {
        return samlMessageSignatureValidator.map(SamlResponseSignatureValidator::new);
    }

    @Provides
    @Singleton
    @Named("CountrySamlAssertionsSignatureValidator")
    private Optional<SamlAssertionsSignatureValidator> getCountrySamlAssertionsSignatureValidator(@Named("CountrySamlMessageSignatureValidator") Optional<SamlMessageSignatureValidator> samlMessageSignatureValidator) {
        return samlMessageSignatureValidator.map(SamlAssertionsSignatureValidator::new);
    }

    @Provides
    @Singleton
    @Named("ResponseAssertionsFromCountryValidator")
    private Optional<ResponseAssertionsFromCountryValidator> getResponseAssertionsFromCountryValidator(
            final ConcurrentMap<String, DateTime> assertionIdCache,
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
                        new DuplicateAssertionValidator(assertionIdCache)
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
                new SignatureRSASHA256(), //TODO: needs to be refactored once TT-71 is complete
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
    private AssertionEncrypter assertionEncrypter(EncryptionCredentialFactory encryptionCredentialFactory) {
        return new AssertionEncrypter(new EncrypterFactory(), encryptionCredentialFactory);
    }

    @Provides
    @Singleton
    private IdpAssertionMetricsCollector metricsCollector(Environment environment) {
        return new IdpAssertionMetricsCollector(environment.metrics());
    }

    private enum KeyPosition {
        PRIMARY,
        SECONDARY
    }

    private PrivateKey privateSigningKey(SamlEngineConfiguration configuration) {
        // Running in production-like environments means we load keys from file descriptors
        // Non-prod environments may load keys from file paths
        if (configuration.shouldReadKeysFromFileDescriptors()) {
            return PrivateKeyFileDescriptors.SIGNING_KEY.loadKey();
        } else {
            return configuration.getPrivateSigningKeyConfiguration().getPrivateKey();
        }
    }

    private Map<KeyPosition, PrivateKey> privateEncryptionKeys(SamlEngineConfiguration configuration) {
        // Running in production-like environments means we load keys from file descriptors
        // Non-prod environments may load keys from file paths
        if (configuration.shouldReadKeysFromFileDescriptors()) {
            return privateEncryptionKeysFromFileDescriptors();
        } else {
            return privateEncryptionKeysFromConfig(configuration);
        }
    }

    private Map<KeyPosition, PrivateKey> privateEncryptionKeysFromConfig(SamlEngineConfiguration configuration) {
        return ImmutableMap.of(
                KeyPosition.PRIMARY, configuration.getPrimaryPrivateEncryptionKeyConfiguration().getPrivateKey(),
                KeyPosition.SECONDARY, configuration.getSecondaryPrivateEncryptionKeyConfiguration().getPrivateKey()
        );
    }

    private Map<KeyPosition, PrivateKey> privateEncryptionKeysFromFileDescriptors() {
        return ImmutableMap.of(
                KeyPosition.PRIMARY, PrivateKeyFileDescriptors.PRIMARY_ENCRYPTION_KEY.loadKey(),
                KeyPosition.SECONDARY, PrivateKeyFileDescriptors.SECONDARY_ENCRYPTION_KEY.loadKey()
        );
    }

}
