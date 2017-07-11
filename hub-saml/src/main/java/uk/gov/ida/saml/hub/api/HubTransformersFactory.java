package uk.gov.ida.saml.hub.api;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.w3c.dom.Element;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.saml.configuration.SamlConfiguration;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.api.CoreTransformersFactory;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.transformers.AuthnContextFactory;
import uk.gov.ida.saml.core.transformers.inbound.decorators.ValidateSamlAuthnRequestFromTransactionDestination;
import uk.gov.ida.saml.core.transformers.outbound.OutboundAssertionToSubjectTransformer;
import uk.gov.ida.saml.core.transformers.outbound.decorators.ResponseAssertionSigner;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlAttributeQueryAssertionEncrypter;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlSignatureSigner;
import uk.gov.ida.saml.core.validators.DestinationValidator;
import uk.gov.ida.saml.core.validators.assertion.AssertionAttributeStatementValidator;
import uk.gov.ida.saml.core.validators.assertion.AssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.AuthnStatementAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.DuplicateAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.IPAddressValidator;
import uk.gov.ida.saml.core.validators.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.MatchingDatasetAssertionValidator;
import uk.gov.ida.saml.core.validators.subject.AssertionSubjectValidator;
import uk.gov.ida.saml.core.validators.subjectconfirmation.AssertionSubjectConfirmationValidator;
import uk.gov.ida.saml.core.validators.subjectconfirmation.BasicAssertionSubjectConfirmationValidator;
import uk.gov.ida.saml.deserializers.ElementToOpenSamlXMLObjectTransformer;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;
import uk.gov.ida.saml.hub.configuration.SamlAuthnRequestValidityDurationConfiguration;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;
import uk.gov.ida.saml.hub.domain.*;
import uk.gov.ida.saml.hub.factories.AttributeFactory_1_1;
import uk.gov.ida.saml.hub.factories.AttributeQueryAttributeFactory;
import uk.gov.ida.saml.hub.transformers.inbound.AuthnRequestFromTransactionUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.AuthnRequestToIdaRequestFromTransactionTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.IdaResponseFromIdpUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.IdpIdaStatusUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.InboundHealthCheckResponseFromMatchingServiceUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.InboundResponseFromMatchingServiceUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatusUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.PassthroughAssertionUnmarshaller;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdpIdaStatusMappingsFactory;
import uk.gov.ida.saml.hub.transformers.inbound.decorators.AssertionSizeValidator;
import uk.gov.ida.saml.hub.transformers.inbound.decorators.AuthnRequestSizeValidator;
import uk.gov.ida.saml.hub.transformers.inbound.decorators.ResponseSizeValidator;
import uk.gov.ida.saml.hub.transformers.inbound.decorators.ValidateSamlResponseIssuedByIdpDestination;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer;
import uk.gov.ida.saml.hub.transformers.inbound.providers.DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.AssertionFromIdpToAssertionTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.AttributeQueryToElementTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.EidasAuthnRequestFromHubToAuthnRequestTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.EncryptedAssertionUnmarshaller;
import uk.gov.ida.saml.hub.transformers.outbound.HubAssertionMarshaller;
import uk.gov.ida.saml.hub.transformers.outbound.HubAttributeQueryRequestToSamlAttributeQueryTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.IdaAuthnRequestFromHubToAuthnRequestTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.MatchingServiceHealthCheckRequestToSamlAttributeQueryTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.OutboundResponseFromHubToSamlResponseTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.RequestAbstractTypeToStringTransformer;
import uk.gov.ida.saml.hub.transformers.outbound.SamlProfileTransactionIdaStatusMarshaller;
import uk.gov.ida.saml.hub.transformers.outbound.TransactionIdaStatusMarshaller;
import uk.gov.ida.saml.hub.transformers.outbound.decorators.NoOpSamlAttributeQueryAssertionEncrypter;
import uk.gov.ida.saml.hub.transformers.outbound.decorators.SamlAttributeQueryAssertionSignatureSigner;
import uk.gov.ida.saml.hub.transformers.outbound.decorators.SigningRequestAbstractTypeSignatureCreator;
import uk.gov.ida.saml.hub.validators.StringSizeValidator;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestFromTransactionValidator;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIdKey;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIssueInstantValidator;
import uk.gov.ida.saml.hub.validators.authnrequest.DuplicateAuthnRequestValidator;
import uk.gov.ida.saml.hub.validators.response.EncryptedResponseFromIdpValidator;
import uk.gov.ida.saml.hub.validators.response.EncryptedResponseFromMatchingServiceValidator;
import uk.gov.ida.saml.hub.validators.response.HealthCheckResponseFromMatchingServiceValidator;
import uk.gov.ida.saml.hub.validators.response.ResponseAssertionsFromIdpValidator;
import uk.gov.ida.saml.hub.validators.response.ResponseAssertionsFromMatchingServiceValidator;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.domain.HubServiceProviderMetadataDto;
import uk.gov.ida.saml.metadata.transformers.AssertionConsumerServicesMarshaller;
import uk.gov.ida.saml.metadata.transformers.ContactPersonsMarshaller;
import uk.gov.ida.saml.metadata.transformers.ContactPersonsUnmarshaller;
import uk.gov.ida.saml.metadata.transformers.EndpointMarshaller;
import uk.gov.ida.saml.metadata.transformers.EntityDescriptorToHubIdentityProviderMetadataDtoValidatingTransformer;
import uk.gov.ida.saml.metadata.transformers.EntityDescriptorToHubTransactionMetadataDtoValidatingTransformer;
import uk.gov.ida.saml.metadata.transformers.HubIdentityProviderMetadataDtoToEntityDescriptorTransformer;
import uk.gov.ida.saml.metadata.transformers.IdentityProviderMetadataMarshaller;
import uk.gov.ida.saml.metadata.transformers.KeyDescriptorFinder;
import uk.gov.ida.saml.metadata.transformers.KeyDescriptorMarshaller;
import uk.gov.ida.saml.metadata.transformers.OrganizationMarshaller;
import uk.gov.ida.saml.metadata.transformers.OrganizationUnmarshaller;
import uk.gov.ida.saml.metadata.transformers.SingleSignOnServicesMarshaller;
import uk.gov.ida.saml.metadata.transformers.TransactionMetadataMarshaller;
import uk.gov.ida.saml.metadata.transformers.ValidUntilExtractor;
import uk.gov.ida.saml.metadata.transformers.decorators.SamlEntityDescriptorValidator;
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
import uk.gov.ida.saml.security.SignatureFactory;
import uk.gov.ida.saml.security.SignatureValidator;
import uk.gov.ida.saml.security.SigningCredentialFactory;
import uk.gov.ida.saml.security.SigningKeyStore;
import uk.gov.ida.saml.security.validators.encryptedelementtype.EncryptionAlgorithmValidator;
import uk.gov.ida.saml.security.validators.issuer.IssuerValidator;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

@SuppressWarnings("unused")
public class HubTransformersFactory {

    private final CoreTransformersFactory coreTransformersFactory;

    public HubTransformersFactory() {
        coreTransformersFactory = new CoreTransformersFactory();
    }

    public Function<Element, HubIdentityProviderMetadataDto> getMetadataForSpTransformer(String hubEntityId) {

        Function<Element, EntityDescriptor> elementToOpenSamlXMLObjectTransformer = getElementToEntityDescriptorTransformer();
        Function<EntityDescriptor, HubIdentityProviderMetadataDto> hubIdentityProviderMetadataDtoValidatingTransformer =
                getEntityDescriptorToHubIdentityProviderMetadataDtoValidatingTransformer(hubEntityId);

        return hubIdentityProviderMetadataDtoValidatingTransformer.compose(elementToOpenSamlXMLObjectTransformer);
    }

    public Function<Element, HubServiceProviderMetadataDto> getElementToHubServiceProviderMetadataDtoTransformer() {


        EntityDescriptorToHubTransactionMetadataDtoValidatingTransformer entityDescriptorToHubTransactionMetadataDtoValidatingTransformer = new EntityDescriptorToHubTransactionMetadataDtoValidatingTransformer(
                new TransactionMetadataMarshaller(
                        new OrganizationMarshaller(),
                        new ContactPersonsMarshaller(),
                        new KeyDescriptorMarshaller(),
                        new KeyDescriptorFinder(),
                        new AssertionConsumerServicesMarshaller(),
                        new ValidUntilExtractor()
                ),
                new SamlEntityDescriptorValidator()
        );

        return entityDescriptorToHubTransactionMetadataDtoValidatingTransformer
                .compose(getElementToEntityDescriptorTransformer());
    }

    public Function<OutboundResponseFromHub, String> getOutboundResponseFromHubToStringTransformer(
            final EncryptionKeyStore encryptionKeyStore,
            final IdaKeyStore keyStore,
            final EntityToEncryptForLocator entityToEncryptForLocator,
            final SignatureAlgorithm signatureAlgorithm,
            final DigestAlgorithm digestAlgorithm) {
        Function<OutboundResponseFromHub, Response> outboundToResponseTransformer = getOutboundResponseFromHubToSamlResponseTransformer();
        Function<Response, String> responseStringTransformer = coreTransformersFactory.getResponseStringTransformer(
                encryptionKeyStore,
                keyStore,
                entityToEncryptForLocator,
                signatureAlgorithm,
                digestAlgorithm);

        return responseStringTransformer.compose(outboundToResponseTransformer);
    }

    public Function<OutboundResponseFromHub, String> getOutboundResponseFromHubToStringTransformer(
            final EncryptionKeyStore encryptionKeyStore,
            final IdaKeyStore keystore,
            final EntityToEncryptForLocator entityToEncryptForLocator,
            final ResponseAssertionSigner responseAssertionSigner,
            final SignatureAlgorithm signatureAlgorithm,
            final DigestAlgorithm digestAlgorithm) {
        Function<OutboundResponseFromHub, Response> outboundToResponseTransformer = getOutboundResponseFromHubToSamlResponseTransformer();
        Function<Response, String> responseStringTransformer = coreTransformersFactory.getResponseStringTransformer(
                encryptionKeyStore,
                keystore,
                entityToEncryptForLocator,
                responseAssertionSigner,
                signatureAlgorithm,
                digestAlgorithm
        );

        return responseStringTransformer.compose(outboundToResponseTransformer);
    }

    public Function<OutboundResponseFromHub, String> getSamlProfileOutboundResponseFromHubToStringTransformer(
            final EncryptionKeyStore encryptionKeyStore,
            final IdaKeyStore keystore,
            final EntityToEncryptForLocator entityToEncryptForLocator,
            final ResponseAssertionSigner responseAssertionSigner,
            final SignatureAlgorithm signatureAlgorithm,
            final DigestAlgorithm digestAlgorithm) {
        Function<OutboundResponseFromHub, Response> outboundToResponseTransformer = getSamlProfileOutboundResponseFromHubToSamlResponseTransformer();
        Function<Response, String> responseStringTransformer = coreTransformersFactory.getResponseStringTransformer(
                encryptionKeyStore,
                keystore,
                entityToEncryptForLocator,
                responseAssertionSigner,
                signatureAlgorithm,
                digestAlgorithm
        );

        return responseStringTransformer.compose(outboundToResponseTransformer);
    }

    public ElementToOpenSamlXMLObjectTransformer<EntityDescriptor> getElementToEntityDescriptorTransformer() {
        return coreTransformersFactory.getElementToOpenSamlXmlObjectTransformer();
    }

    public Function<HubIdentityProviderMetadataDto, Element> getHubIdentityProviderMetadataDtoToElementTransformer() {
        return
                coreTransformersFactory.<EntityDescriptor>getXmlObjectToElementTransformer().compose(getHubIdentityProviderMetadataDtoToEntityDescriptorTransformer());
    }

    public Function<HubServiceProviderMetadataDto, Element> getHubServiceProviderMetadataDtoToElementTransformer() {
        return coreTransformersFactory.<EntityDescriptor>getXmlObjectToElementTransformer().compose(new CoreTransformersFactory().getHubServiceProviderMetadataDtoToEntityDescriptorTransformer());
    }

    public Function<IdaAuthnRequestFromHub, String> getIdaAuthnRequestFromHubToStringTransformer(IdaKeyStore keyStore, SignatureAlgorithm signatureAlgorithm, DigestAlgorithm digestAlgorithm) {
        return getAuthnRequestToStringTransformer(false, keyStore, signatureAlgorithm, digestAlgorithm).compose(getIdaAuthnRequestFromHubToAuthnRequestTransformer());
    }

    public Function<EidasAuthnRequestFromHub, String> getEidasAuthnRequestFromHubToStringTransformer(IdaKeyStore keyStore, SignatureAlgorithm signatureAlgorithm, DigestAlgorithm digestAlgorithm) {
        return getAuthnRequestToStringTransformer(true, keyStore, signatureAlgorithm, digestAlgorithm).compose(getEidasAuthnRequestFromHubToAuthnRequestTransformer());
    }

    public Function<String, AuthnRequestFromTransaction> getStringToIdaAuthnRequestTransformer(
            SamlConfiguration samlConfiguration,
            SigningKeyStore signingKeyStore,
            ConcurrentMap<AuthnRequestIdKey, DateTime> duplicateIds,
            SamlDuplicateRequestValidationConfiguration samlDuplicateRequestValidationConfiguration,
            SamlAuthnRequestValidityDurationConfiguration samlAuthnRequestValidityDurationConfiguration
    ) {
        Function<String, AuthnRequest> stringToAuthnRequestTransformer = getStringToAuthnRequestTransformer();
        Function<AuthnRequest, AuthnRequestFromTransaction> authnRequestToIdaRequestFromTransactionTransformer =
                getAuthnRequestToAuthnRequestFromTransactionTransformer(samlConfiguration, signingKeyStore, duplicateIds, samlDuplicateRequestValidationConfiguration,
                        samlAuthnRequestValidityDurationConfiguration);

        return authnRequestToIdaRequestFromTransactionTransformer.compose(stringToAuthnRequestTransformer);
    }

    public StringToOpenSamlObjectTransformer<AuthnRequest> getStringToAuthnRequestTransformer() {
        return coreTransformersFactory.getStringtoOpenSamlObjectTransformer(
                new AuthnRequestSizeValidator(new StringSizeValidator())
        );
    }

    public StringToOpenSamlObjectTransformer<Response> getStringToResponseTransformer() {
        return coreTransformersFactory.getStringtoOpenSamlObjectTransformer(
                new ResponseSizeValidator(new StringSizeValidator())
        );
    }

    public StringToOpenSamlObjectTransformer<Response> getStringToResponseTransformer(ResponseSizeValidator validator) {
        return coreTransformersFactory.getStringtoOpenSamlObjectTransformer(
                validator
        );
    }

    public StringToOpenSamlObjectTransformer<Assertion> getStringToAssertionTransformer() {
        return coreTransformersFactory.getStringtoOpenSamlObjectTransformer(
                new AssertionSizeValidator()
        );
    }

    public PassthroughAssertionUnmarshaller getAssertionToPassthroughAssertionTransformer() {
        return new PassthroughAssertionUnmarshaller(
                new XmlObjectToBase64EncodedStringTransformer<>(),
                new AuthnContextFactory()
        );
    }

    public AssertionFromIdpToAssertionTransformer getAssertionFromIdpToAssertionTransformer() {
        return new AssertionFromIdpToAssertionTransformer(getStringToAssertionTransformer());
    }

    public Function<HubAttributeQueryRequest, Element> getMatchingServiceRequestToElementTransformer(
            IdaKeyStore keyStore,
            EncryptionKeyStore encryptionKeyStore,
            EntityToEncryptForLocator entity,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm, String hubEntityId) {
        Function<HubAttributeQueryRequest, AttributeQuery> t1 = getHubAttributeQueryRequestToSamlAttributeQueryTransformer();
        Function<AttributeQuery, Element> t2 = getAttributeQueryToElementTransformer(keyStore, encryptionKeyStore, Optional.fromNullable(entity), signatureAlgorithm, digestAlgorithm, hubEntityId);

        return t2.compose(t1);
    }

    public Function<MatchingServiceHealthCheckRequest, Element> getMatchingServiceHealthCheckRequestToElementTransformer(
            IdaKeyStore keyStore,
            EncryptionKeyStore encryptionKeyStore,
            EntityToEncryptForLocator entity,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm, String hubEntityId) {
        Function<MatchingServiceHealthCheckRequest, AttributeQuery> t1
                = new MatchingServiceHealthCheckRequestToSamlAttributeQueryTransformer(new OpenSamlXmlObjectFactory());
        Function<AttributeQuery, Element> attributeQueryToElementTransformer = getAttributeQueryToElementTransformer(keyStore, encryptionKeyStore, Optional.fromNullable(entity), signatureAlgorithm, digestAlgorithm, hubEntityId);
        return attributeQueryToElementTransformer.compose(t1);
    }

    public <T extends RequestAbstractType> RequestAbstractTypeToStringTransformer<T> getRequestAbstractTypeToStringTransformer(
            boolean includeKeyInfo,
            IdaKeyStore keyStore,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm) {
        return new RequestAbstractTypeToStringTransformer<>(
                new SigningRequestAbstractTypeSignatureCreator<T>(new SignatureFactory(includeKeyInfo, new IdaKeyStoreCredentialRetriever(keyStore), signatureAlgorithm, digestAlgorithm)),
                new SamlSignatureSigner<T>(),
                new XmlObjectToBase64EncodedStringTransformer<>()
        );
    }

    public RequestAbstractTypeToStringTransformer<AuthnRequest> getAuthnRequestToStringTransformer(
            boolean includeKeyInfo,
            IdaKeyStore keyStore,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm) {
        return getRequestAbstractTypeToStringTransformer(includeKeyInfo, keyStore, signatureAlgorithm, digestAlgorithm);
    }

    public DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer getResponseToInboundResponseFromMatchingServiceTransformer(
            SigningKeyStore signingKeyStore,
            IdaKeyStore keyStore, String hubEntityId) {
        SigningCredentialFactory signingCredentialFactory = new SigningCredentialFactory(signingKeyStore);
        return new DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer(
                new InboundResponseFromMatchingServiceUnmarshaller(
                        getAssertionToPassthroughAssertionTransformer(),
                        new MatchingServiceIdaStatusUnmarshaller()
                ),
                this.<InboundResponseFromMatchingService>getSamlResponseSignatureValidator(signingCredentialFactory),
                this.<InboundResponseFromMatchingService>getSamlResponseAssertionDecrypter(keyStore),
                new SamlAssertionsSignatureValidator(new SamlMessageSignatureValidator(new CredentialFactorySignatureValidator(signingCredentialFactory))),
                new EncryptedResponseFromMatchingServiceValidator(),
                new ResponseAssertionsFromMatchingServiceValidator(
                        new AssertionValidator(
                                new IssuerValidator(),
                                new AssertionSubjectValidator(),
                                new AssertionAttributeStatementValidator(),
                                new BasicAssertionSubjectConfirmationValidator()
                        ),
                        hubEntityId
                )
        );
    }

    public Function<String, InboundResponseFromIdp> getStringToIdaResponseIssuedByIdpTransformer(
            SigningKeyStore signingKeyStore,
            IdaKeyStore keyStore,
            SamlConfiguration samlConfiguration,
            String expectedEndpoint,
            ConcurrentMap<String, DateTime> assertionIdCache,
            String hubEntityId) {
        // not sure if we need to allow an extra ResponseSizeValidator here.
        Function<String, Response> t1 = getStringToResponseTransformer();
        Function<Response, InboundResponseFromIdp> t2 = getDecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(
                signingKeyStore,
                keyStore,
                samlConfiguration,
                expectedEndpoint,
                assertionIdCache,
                hubEntityId);
        return  t2.compose(t1);
    }

    public DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer getDecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(
            SigningKeyStore signingKeyStore,
            IdaKeyStore keyStore,
            SamlConfiguration samlConfiguration,
            String expectedEndpoint,
            ConcurrentMap<String, DateTime> assertionIdCache,
            String hubEntityId) {
        SigningCredentialFactory signingCredentialFactory = new SigningCredentialFactory(signingKeyStore);
        return new DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(
                new IdaResponseFromIdpUnmarshaller(
                        new IdpIdaStatusUnmarshaller(new IdpIdaStatus.IdpIdaStatusFactory(), new SamlStatusToIdpIdaStatusMappingsFactory()),
                        getAssertionToPassthroughAssertionTransformer()
                ),
                this.<InboundResponseFromIdp>getSamlResponseSignatureValidator(signingCredentialFactory),
                this.<InboundResponseFromIdp>getSamlResponseAssertionDecrypter(keyStore),
                new SamlAssertionsSignatureValidator(new SamlMessageSignatureValidator(new CredentialFactorySignatureValidator(signingCredentialFactory))),
                new EncryptedResponseFromIdpValidator(new SamlStatusToIdpIdaStatusMappingsFactory()),
                new ValidateSamlResponseIssuedByIdpDestination(
                        new DestinationValidator(samlConfiguration.getExpectedDestinationHost()),
                        expectedEndpoint),
                getResponseAssertionsFromIdpValidator(assertionIdCache, hubEntityId)
        );
    }

    public AuthnRequestToIdaRequestFromTransactionTransformer getAuthnRequestToAuthnRequestFromTransactionTransformer(final SamlConfiguration samlConfiguration,
                                                                                                                      final SigningKeyStore signingKeyStore, final ConcurrentMap<AuthnRequestIdKey, DateTime> duplicateIds,
                                                                                                                      final SamlDuplicateRequestValidationConfiguration samlDuplicateRequestValidationConfiguration,
                                                                                                                      final SamlAuthnRequestValidityDurationConfiguration samlAuthnRequestValidityDurationConfiguration) {
        return new AuthnRequestToIdaRequestFromTransactionTransformer(
                new AuthnRequestFromTransactionUnmarshaller(),
                coreTransformersFactory.<AuthnRequest>getSamlRequestSignatureValidator(signingKeyStore),
                new ValidateSamlAuthnRequestFromTransactionDestination(new DestinationValidator(samlConfiguration.getExpectedDestinationHost()), Endpoints.SSO_REQUEST_ENDPOINT),
                new AuthnRequestFromTransactionValidator(
                        new IssuerValidator(),
                        new DuplicateAuthnRequestValidator(duplicateIds, samlDuplicateRequestValidationConfiguration),
                        new AuthnRequestIssueInstantValidator(samlAuthnRequestValidityDurationConfiguration)
                )
        );
    }

    private OutboundResponseFromHubToSamlResponseTransformer getOutboundResponseFromHubToSamlResponseTransformer() {
        return new OutboundResponseFromHubToSamlResponseTransformer(
                new TransactionIdaStatusMarshaller(new OpenSamlXmlObjectFactory()),
                new OpenSamlXmlObjectFactory(),
                new AssertionFromIdpToAssertionTransformer(getStringToAssertionTransformer())
        );
    }

    private OutboundResponseFromHubToSamlResponseTransformer getSamlProfileOutboundResponseFromHubToSamlResponseTransformer() {
        return new OutboundResponseFromHubToSamlResponseTransformer(
                new SamlProfileTransactionIdaStatusMarshaller(new OpenSamlXmlObjectFactory()),
                new OpenSamlXmlObjectFactory(),
                new AssertionFromIdpToAssertionTransformer(getStringToAssertionTransformer())
        );
    }

    private HubIdentityProviderMetadataDtoToEntityDescriptorTransformer getHubIdentityProviderMetadataDtoToEntityDescriptorTransformer() {
        OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
        return new HubIdentityProviderMetadataDtoToEntityDescriptorTransformer(
                openSamlXmlObjectFactory,
                new OrganizationUnmarshaller(openSamlXmlObjectFactory),
                new ContactPersonsUnmarshaller(openSamlXmlObjectFactory),
                coreTransformersFactory.getCertificatesToKeyDescriptorsTransformer(),
                new IdGenerator()
        );
    }

    private IdaAuthnRequestFromHubToAuthnRequestTransformer getIdaAuthnRequestFromHubToAuthnRequestTransformer() {
        return new IdaAuthnRequestFromHubToAuthnRequestTransformer(new OpenSamlXmlObjectFactory());
    }

    private EidasAuthnRequestFromHubToAuthnRequestTransformer getEidasAuthnRequestFromHubToAuthnRequestTransformer() {
        return new EidasAuthnRequestFromHubToAuthnRequestTransformer(new OpenSamlXmlObjectFactory());
    }

    private HubAttributeQueryRequestToSamlAttributeQueryTransformer getHubAttributeQueryRequestToSamlAttributeQueryTransformer() {
        return new HubAttributeQueryRequestToSamlAttributeQueryTransformer(
                new OpenSamlXmlObjectFactory(),
                new HubAssertionMarshaller(
                        new OpenSamlXmlObjectFactory(),
                        new AttributeFactory_1_1(new OpenSamlXmlObjectFactory()),
                        new OutboundAssertionToSubjectTransformer(new OpenSamlXmlObjectFactory())),
                new AssertionFromIdpToAssertionTransformer(
                        getStringToAssertionTransformer()
                ),
                new AttributeQueryAttributeFactory(new OpenSamlXmlObjectFactory()),
                new EncryptedAssertionUnmarshaller(getStringToEncryptedAssertionTransformer()));
    }

    private StringToOpenSamlObjectTransformer<EncryptedAssertion> getStringToEncryptedAssertionTransformer() {
        return coreTransformersFactory.getStringtoOpenSamlObjectTransformer(
                new AssertionSizeValidator()
        );
    }

    private AttributeQueryToElementTransformer getAttributeQueryToElementTransformer(
            IdaKeyStore keyStore,
            EncryptionKeyStore encryptionKeyStore,
            Optional<EntityToEncryptForLocator> entity,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm,
            String hubEntityId) {
        return new AttributeQueryToElementTransformer(
                new SigningRequestAbstractTypeSignatureCreator<>(new SignatureFactory(new IdaKeyStoreCredentialRetriever(keyStore), signatureAlgorithm, digestAlgorithm)),
                new SamlAttributeQueryAssertionSignatureSigner(new IdaKeyStoreCredentialRetriever(keyStore), new OpenSamlXmlObjectFactory(), hubEntityId),
                new SamlSignatureSigner<AttributeQuery>(),
                new XmlObjectToElementTransformer<>(),
                getSamlAttributeQueryAssertionEncrypter(encryptionKeyStore, entity)
        );
    }

    private SamlAttributeQueryAssertionEncrypter getSamlAttributeQueryAssertionEncrypter(EncryptionKeyStore encryptionKeyStore, Optional<EntityToEncryptForLocator> entity) {
        if (entity.isPresent()) {
            return new SamlAttributeQueryAssertionEncrypter(new EncryptionCredentialFactory(encryptionKeyStore), new EncrypterFactory(), entity.get());
        } else {
            return new NoOpSamlAttributeQueryAssertionEncrypter();
        }
    }

    public EntityDescriptorToHubIdentityProviderMetadataDtoValidatingTransformer getEntityDescriptorToHubIdentityProviderMetadataDtoValidatingTransformer(String hubEntityId) {
        IdentityProviderMetadataMarshaller identityProviderMetadataMarshaller = getEntityDescriptorToHubIdentityProviderMetadataDtoTransformer(hubEntityId);
        SamlEntityDescriptorValidator entityDescriptorValidator = new SamlEntityDescriptorValidator();

        return new EntityDescriptorToHubIdentityProviderMetadataDtoValidatingTransformer(identityProviderMetadataMarshaller, entityDescriptorValidator);
    }

    private IdentityProviderMetadataMarshaller getEntityDescriptorToHubIdentityProviderMetadataDtoTransformer(String hubEntityId) {
        SingleSignOnServicesMarshaller singleSignOnServiceTransformer = new SingleSignOnServicesMarshaller(new EndpointMarshaller());
        return new IdentityProviderMetadataMarshaller(
                new OrganizationMarshaller(),
                new ContactPersonsMarshaller(),
                singleSignOnServiceTransformer,
                new KeyDescriptorMarshaller(),
                new ValidUntilExtractor(),
                hubEntityId);
    }

    private ResponseAssertionsFromIdpValidator getResponseAssertionsFromIdpValidator(final ConcurrentMap<String, DateTime> assertionIdCache, String hubEntityId) {
        return new ResponseAssertionsFromIdpValidator(
                new IdentityProviderAssertionValidator(
                        new IssuerValidator(),
                        new AssertionSubjectValidator(),
                        new AssertionAttributeStatementValidator(),
                        new AssertionSubjectConfirmationValidator()
                ),
                new MatchingDatasetAssertionValidator(new DuplicateAssertionValidator(assertionIdCache)),
                new AuthnStatementAssertionValidator(
                        new DuplicateAssertionValidator(assertionIdCache)
                ),
                new IPAddressValidator(),
                hubEntityId
        );
    }

    public DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer getResponseInboundHealthCheckResponseFromMatchingServiceTransformer(SigningKeyStore signingKeyStore) {
        SigningCredentialFactory signingCredentialFactory = new SigningCredentialFactory(signingKeyStore);
        return new DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer(
                new InboundHealthCheckResponseFromMatchingServiceUnmarshaller(
                        new MatchingServiceIdaStatusUnmarshaller()
                ),
                this.<InboundHealthCheckResponseFromMatchingService>getSamlResponseSignatureValidator(signingCredentialFactory),
                new HealthCheckResponseFromMatchingServiceValidator(
                )

        );
    }

    private AssertionDecrypter getSamlResponseAssertionDecrypter(IdaKeyStore keyStore) {
        return new AssertionDecrypter(
                new IdaKeyStoreCredentialRetriever(keyStore), new EncryptionAlgorithmValidator(), new DecrypterFactory()
        );
    }

    private SamlResponseSignatureValidator getSamlResponseSignatureValidator(SigningCredentialFactory signingCredentialFactory) {
        SignatureValidator signatureValidator = coreTransformersFactory.getSignatureValidator(signingCredentialFactory);
        return new SamlResponseSignatureValidator(new SamlMessageSignatureValidator(signatureValidator));
    }
}
