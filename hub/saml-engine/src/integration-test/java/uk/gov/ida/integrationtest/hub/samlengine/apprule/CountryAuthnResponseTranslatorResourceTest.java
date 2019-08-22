package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.impl.AttributeStatementBuilder;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.samlengine.domain.InboundResponseFromCountry;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.integrationtest.hub.samlengine.support.AssertionDecrypter;
import uk.gov.ida.saml.core.extensions.EidasAuthnContext;
import uk.gov.ida.saml.core.extensions.eidas.PersonIdentifier;
import uk.gov.ida.saml.core.extensions.eidas.impl.PersonIdentifierBuilder;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.core.test.builders.AudienceRestrictionBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnContextBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnContextClassRefBuilder;
import uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder;
import uk.gov.ida.saml.core.test.builders.ConditionsBuilder;
import uk.gov.ida.saml.core.test.builders.IssuerBuilder;
import uk.gov.ida.saml.core.test.builders.PersonIdentifierAttributeBuilder;
import uk.gov.ida.saml.core.test.builders.ResponseBuilder;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.core.test.builders.StatusBuilder;
import uk.gov.ida.saml.core.test.builders.StatusCodeBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder;
import uk.gov.ida.saml.hub.api.HubTransformersFactory;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;
import uk.gov.ida.saml.hub.factories.EidasAttributeFactory;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;
import static uk.gov.ida.saml.core.test.builders.NameIdBuilder.aNameId;

public class CountryAuthnResponseTranslatorResourceTest {

    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA256();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();
    private final static String matchingServiceEntityId = TEST_RP_MS;
    public static final String DESTINATION = "http://localhost" + Urls.FrontendUrls.SAML2_SSO_EIDAS_RESPONSE_ENDPOINT;
    public static final String SUCCESS_STATUS_CODE = StatusCode.SUCCESS;

    private static Client client;

    @ClassRule
    public static final ConfigStubRule configStubRule = new ConfigStubRule();

    @ClassRule
    public static final SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            ConfigOverride.config("configUri", configStubRule.baseUri().build().toASCIIString()),
            ConfigOverride.config("country.saml.expectedDestination", DESTINATION)
    );

    @Before
    public void setUp() throws Exception {
        configStubRule.setupCertificatesForEntity(TEST_RP_MS);
    }

    @BeforeClass
    public static void setUpClient() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(CountryAuthnResponseTranslatorResourceTest.class.getSimpleName());
    }

    @Test
    public void shouldReturnSuccessResponse() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY);
        org.opensaml.saml.saml2.core.Response originalAuthnResponse = new HubTransformersFactory().getStringToResponseTransformer().apply(dto.getSamlResponse());

        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromCountry inboundResponseFromCountry = response.readEntity(InboundResponseFromCountry.class);
        assertThat(inboundResponseFromCountry.getStatus()).isEqualTo(Optional.of("Success"));
        assertThat(inboundResponseFromCountry.getIssuer()).isEqualTo(samlEngineAppRule.getCountryMetadataUri());
        assertThatDecryptedAssertionsAreTheSame(inboundResponseFromCountry, originalAuthnResponse);
    }

    @Test
    public void shouldReturnNonSuccessResponse() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createUnsuccessfulAuthnResponseSignedByKeyPair(StatusCode.REQUESTER, StatusCode.REQUEST_DENIED);

        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromCountry inboundResponseFromCountry = response.readEntity(InboundResponseFromCountry.class);

        assertThat(inboundResponseFromCountry.getStatus()).isEqualTo(Optional.of(CountryAuthenticationStatus.Status.Failure.name()));
        assertThat(inboundResponseFromCountry.getIssuer()).isEqualTo(samlEngineAppRule.getCountryMetadataUri());
        assertThat(inboundResponseFromCountry.getEncryptedIdentityAssertionBlob().isPresent()).isEqualTo(false);
    }

    @Test
    public void shouldReturnErrorWhenValidatingEidasAuthnResponseContainingInvalidSignature() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(TestCertificateStrings.METADATA_SIGNING_B_PUBLIC_CERT, TestCertificateStrings.METADATA_SIGNING_B_PRIVATE_KEY);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnSuccessResponseWhenAES256GCMAlgorithmIsUsed() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT,
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void shouldReturnSuccessResponseWhenAES128GCMAlgorithmIsUsed() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT,
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void shouldReturnSuccessResponseWhenRSAOAEPMGF1PKeyTransportAlgorithmIsUsed() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT,
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void shouldReturnSuccessResponseWhenRSAOAEPKeyTransportAlgorithmIsUsed() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT,
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void shouldReturnErrorResponseWhenUnsupportedAlgorithmIsUsed() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT,
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES192_GCM,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnErrorResponseWhenUnsupportedKeyTransportAlgorithmUsed() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT,
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSA15);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnACountryResponseDtoWithAreAssertionsUnsignedEqualsFalseWhenSigned() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPair(
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT,
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11);
        Response response = postAuthnResponseToSamlEngine(dto);

        assertThat((response.readEntity(InboundResponseFromCountry.class)).areAssertionsUnsigned()).isEqualTo(false);    }

    @Test
    public void shouldReturnACountryResponseDtoWithAreAssertionsUnsignedEqualsTrueWhenUnsigned() throws Exception {
        SamlAuthnResponseTranslatorDto dto = createAuthnResponseSignedByKeyPairAndUnsignedAssertions(
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT,
                TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES128_GCM,
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11);
        Response response = postAuthnResponseToSamlEngine(dto);
        InboundResponseFromCountry inboundResponseFromCountry = response.readEntity(InboundResponseFromCountry.class);
        assertThat(inboundResponseFromCountry.areAssertionsUnsigned()).isEqualTo(true);    }

    private void assertThatDecryptedAssertionsAreTheSame(InboundResponseFromCountry response, org.opensaml.saml.saml2.core.Response originalResponse) {
        AssertionDecrypter hubDecrypter = new AssertionDecrypter(TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY, TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT);
        List<Assertion> originalAssertions = hubDecrypter.decryptAssertions(originalResponse);

        AssertionDecrypter rpDecrypter = new AssertionDecrypter(TestCertificateStrings.TEST_RP_MS_PRIVATE_ENCRYPTION_KEY, TestCertificateStrings.TEST_RP_PUBLIC_ENCRYPTION_CERT);
        Assertion returnedAssertion = rpDecrypter.decryptAssertion(response.getEncryptedIdentityAssertionBlob().get());

        assertThat(originalAssertions).hasSize(1);
        Assertion originalAssertion = originalAssertions.get(0);
        assertEquals(returnedAssertion, originalAssertion);
    }

    private void assertEquals(Assertion first, Assertion second) {
        XmlObjectToBase64EncodedStringTransformer serializer = new XmlObjectToBase64EncodedStringTransformer();
        assertThat(serializer.apply(first)).isEqualTo(serializer.apply(second));
    }

    private SamlAuthnResponseTranslatorDto createAuthnResponseSignedByKeyPair(String publicKey, String privateKey) throws Exception {
        return createAuthnResponseSignedByKeyPair(publicKey, privateKey, EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM, EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);
    }

    private SamlAuthnResponseTranslatorDto createAuthnResponseSignedByKeyPair(String publicKey, String privateKey, String contentEncrytionAlgorithm, String keyTransportEncryptionAlgorithm) throws Exception {
        return createAuthnResponseSignedByKeyPair(publicKey, privateKey, contentEncrytionAlgorithm, keyTransportEncryptionAlgorithm,
                SUCCESS_STATUS_CODE, null, true);
    }

    private SamlAuthnResponseTranslatorDto createAuthnResponseSignedByKeyPairAndUnsignedAssertions(String publicKey, String privateKey, String contentEncrytionAlgorithm, String keyTransportEncryptionAlgorithm) throws Exception {
        return createAuthnResponseSignedByKeyPairWithUnsignedAssertions(publicKey, privateKey, contentEncrytionAlgorithm, keyTransportEncryptionAlgorithm,
                SUCCESS_STATUS_CODE, null, true);
    }

    private SamlAuthnResponseTranslatorDto createUnsuccessfulAuthnResponseSignedByKeyPair(String statusCode, String subStatusCode) throws Exception {
        return createAuthnResponseSignedByKeyPair(TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY,
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256_GCM, EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP,
                statusCode, subStatusCode, false);
    }

    private SamlAuthnResponseTranslatorDto createAuthnResponseSignedByKeyPair(
            String publicKey, String privateKey, String contentEncryptionAlgorithm, String keyTransportEncryptionAlgorithm, String statusCode, String subStatusCode, boolean includeEncryptedAssertion) throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        String samlResponse = aResponseFromCountry("a-request",
                samlEngineAppRule.getCountryMetadataUri(),
                publicKey,
                privateKey,
                DESTINATION,
                statusCode,
                subStatusCode,
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM,
                contentEncryptionAlgorithm,
                EidasAuthnContext.EIDAS_LOA_SUBSTANTIAL,
                DESTINATION,
                samlEngineAppRule.getCountryMetadataUri(),
                keyTransportEncryptionAlgorithm,
                includeEncryptedAssertion);
        return new SamlAuthnResponseTranslatorDto(samlResponse, sessionId, "127.0.0.1", matchingServiceEntityId);
    }

    private SamlAuthnResponseTranslatorDto createAuthnResponseSignedByKeyPairWithUnsignedAssertions(
            String publicKey, String privateKey, String contentEncryptionAlgorithm, String keyTransportEncryptionAlgorithm, String statusCode, String subStatusCode, boolean includeEncryptedAssertion) throws Exception {
        SessionId sessionId = SessionId.createNewSessionId();
        String samlResponse = aResponseFromCountryWithUnsignedAssertions("a-request",
                samlEngineAppRule.getCountryMetadataUri(),
                publicKey,
                privateKey,
                DESTINATION,
                statusCode,
                subStatusCode,
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM,
                contentEncryptionAlgorithm,
                EidasAuthnContext.EIDAS_LOA_SUBSTANTIAL,
                DESTINATION,
                samlEngineAppRule.getCountryMetadataUri(),
                keyTransportEncryptionAlgorithm,
                includeEncryptedAssertion);
        return new SamlAuthnResponseTranslatorDto(samlResponse, sessionId, "127.0.0.1", matchingServiceEntityId);
    }

    private Response postAuthnResponseToSamlEngine(SamlAuthnResponseTranslatorDto authnResponse) {
        return postToSamlEngineAuthnResponseSignedBy(authnResponse, samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_COUNTRY_AUTHN_RESPONSE_RESOURCE));
    }

    private Response postToSamlEngineAuthnResponseSignedBy(SamlAuthnResponseTranslatorDto dto, URI uri) {
        return client
                .target(uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(dto));
    }

    public String aResponseFromCountry(
            String requestId,
            String idpEntityId,
            String publicCert,
            String privateKey,
            String destination,
            String statusCode,
            String subStatusCode,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm,
            String encryptionAlgorithm,
            String authnContext,
            String recipient,
            String audienceId,
            String keyTransportAlgorithm,
            boolean includeEncryptedAssertion) throws Exception {
        TestCredentialFactory hubEncryptionCredentialFactory =
                new TestCredentialFactory(TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT, TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY);
        TestCredentialFactory idpSigningCredentialFactory = new TestCredentialFactory(publicCert, privateKey);

        final String persistentId = "UK/GB/12345";
        final Subject authnAssertionSubject =
                SubjectBuilder.aSubject()
                        .withNameId(aNameId().withValue(persistentId).build())
                        .withSubjectConfirmation(
                                SubjectConfirmationBuilder.aSubjectConfirmation()
                                        .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                                                .withInResponseTo(requestId)
                                                .withRecipient(recipient)
                                                .build())
                                        .build())
                        .build();
        final Conditions conditions =
                ConditionsBuilder.aConditions()
                        .addAudienceRestriction(
                                AudienceRestrictionBuilder.anAudienceRestriction()
                                        .withAudienceId(audienceId)
                                        .build()
                        ).build();
        final AuthnStatement authnStatement = AuthnStatementBuilder.anAuthnStatement()
                .withAuthnContext(AuthnContextBuilder.anAuthnContext()
                        .withAuthnContextClassRef(
                                AuthnContextClassRefBuilder.anAuthnContextClassRef().
                                        withAuthnContextClasRefValue(authnContext)
                                        .build())
                        .build())
                .build();

        Attribute firstNameAttribute = new EidasAttributeFactory().createFirstNameAttribute("Javier");
        Attribute familyNameAttribute = new EidasAttributeFactory().createFamilyName("Garcia");
        Attribute dateOfBirthAttribute = new EidasAttributeFactory().createDateOfBirth(new LocalDate("1965-01-01"));

        PersonIdentifier personIdentifier = new PersonIdentifierBuilder().buildObject();
        personIdentifier.setPersonIdentifier(persistentId);
        Attribute personIdentifierAttribute = PersonIdentifierAttributeBuilder.aPersonIdentifier().withValue(personIdentifier).build();

        Status status = StatusBuilder.aStatus().withStatusCode(
                StatusCodeBuilder.aStatusCode().withValue(statusCode).withSubStatusCode(
                        StatusCodeBuilder.aStatusCode().withValue(subStatusCode)
                                .build())
                        .build())
                .build();

        final AttributeStatement attributeStatement = new AttributeStatementBuilder().buildObject();
        attributeStatement.getAttributes().addAll(Arrays.asList(firstNameAttribute, familyNameAttribute, dateOfBirthAttribute, personIdentifierAttribute));

        final Credential encryptingCredential = hubEncryptionCredentialFactory.getEncryptingCredential();
        final Credential signingCredential = idpSigningCredentialFactory.getSigningCredential();

        final String assertionID = UUID.randomUUID().toString();

        final ResponseBuilder responseBuilder = ResponseBuilder.aResponse()
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withSigningCredential(signingCredential)
                .withSignatureAlgorithm(signatureAlgorithm)
                .withDigestAlgorithm(digestAlgorithm)
                .withInResponseTo(requestId)
                .withDestination(destination)
                .withNoDefaultAssertion()
                .withStatus(status);

        if (includeEncryptedAssertion) {
            responseBuilder
                    .addEncryptedAssertion(
                            AssertionBuilder.anAssertion()
                                    .withId(assertionID)
                                    .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                                    .withSubject(authnAssertionSubject)
                                    .withConditions(conditions)
                                    .addAuthnStatement(authnStatement)
                                    .addAttributeStatement(attributeStatement)
                                    .withSignature(SignatureBuilder.aSignature()
                                            .withSigningCredential(signingCredential)
                                            .withSignatureAlgorithm(signatureAlgorithm)
                                            .withDigestAlgorithm(assertionID, digestAlgorithm).build())
                                    .buildWithEncrypterCredential(encryptingCredential, encryptionAlgorithm, keyTransportAlgorithm));
        }

        return new XmlObjectToBase64EncodedStringTransformer<>().apply(responseBuilder.build());
    }

    public String aResponseFromCountryWithUnsignedAssertions(
            String requestId,
            String idpEntityId,
            String publicCert,
            String privateKey,
            String destination,
            String statusCode,
            String subStatusCode,
            SignatureAlgorithm signatureAlgorithm,
            DigestAlgorithm digestAlgorithm,
            String encryptionAlgorithm,
            String authnContext,
            String recipient,
            String audienceId,
            String keyTransportAlgorithm,
            boolean includeEncryptedAssertion) throws Exception {
        TestCredentialFactory hubEncryptionCredentialFactory =
                new TestCredentialFactory(TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT, TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY);
        TestCredentialFactory idpSigningCredentialFactory = new TestCredentialFactory(publicCert, privateKey);

        final String persistentId = "UK/GB/12345";
        final Subject authnAssertionSubject =
                SubjectBuilder.aSubject()
                        .withNameId(aNameId().withValue(persistentId).build())
                        .withSubjectConfirmation(
                                SubjectConfirmationBuilder.aSubjectConfirmation()
                                        .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                                                .withInResponseTo(requestId)
                                                .withRecipient(recipient)
                                                .build())
                                        .build())
                        .build();
        final Conditions conditions =
                ConditionsBuilder.aConditions()
                        .addAudienceRestriction(
                                AudienceRestrictionBuilder.anAudienceRestriction()
                                        .withAudienceId(audienceId)
                                        .build()
                        ).build();
        final AuthnStatement authnStatement = AuthnStatementBuilder.anAuthnStatement()
                .withAuthnContext(AuthnContextBuilder.anAuthnContext()
                        .withAuthnContextClassRef(
                                AuthnContextClassRefBuilder.anAuthnContextClassRef().
                                        withAuthnContextClasRefValue(authnContext)
                                        .build())
                        .build())
                .build();

        Attribute firstNameAttribute = new EidasAttributeFactory().createFirstNameAttribute("Javier");
        Attribute familyNameAttribute = new EidasAttributeFactory().createFamilyName("Garcia");
        Attribute dateOfBirthAttribute = new EidasAttributeFactory().createDateOfBirth(new LocalDate("1965-01-01"));

        PersonIdentifier personIdentifier = new PersonIdentifierBuilder().buildObject();
        personIdentifier.setPersonIdentifier(persistentId);
        Attribute personIdentifierAttribute = PersonIdentifierAttributeBuilder.aPersonIdentifier().withValue(personIdentifier).build();

        Status status = StatusBuilder.aStatus().withStatusCode(
                StatusCodeBuilder.aStatusCode().withValue(statusCode).withSubStatusCode(
                        StatusCodeBuilder.aStatusCode().withValue(subStatusCode)
                                .build())
                        .build())
                .build();

        final AttributeStatement attributeStatement = new AttributeStatementBuilder().buildObject();
        attributeStatement.getAttributes().addAll(Arrays.asList(firstNameAttribute, familyNameAttribute, dateOfBirthAttribute, personIdentifierAttribute));

        final Credential encryptingCredential = hubEncryptionCredentialFactory.getEncryptingCredential();
        final Credential signingCredential = idpSigningCredentialFactory.getSigningCredential();

        final String assertionID = UUID.randomUUID().toString();

        final ResponseBuilder responseBuilder = ResponseBuilder.aResponse()
                .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                .withSigningCredential(signingCredential)
                .withSignatureAlgorithm(signatureAlgorithm)
                .withDigestAlgorithm(digestAlgorithm)
                .withInResponseTo(requestId)
                .withDestination(destination)
                .withNoDefaultAssertion()
                .withStatus(status);

        if (includeEncryptedAssertion) {
            responseBuilder
                    .addEncryptedAssertion(
                            AssertionBuilder.anAssertion()
                                    .withId(assertionID)
                                    .withIssuer(IssuerBuilder.anIssuer().withIssuerId(idpEntityId).build())
                                    .withSubject(authnAssertionSubject)
                                    .withConditions(conditions)
                                    .addAuthnStatement(authnStatement)
                                    .addAttributeStatement(attributeStatement)
                                    .withSignature(null)
                                    .buildWithEncrypterCredential(encryptingCredential, encryptionAlgorithm, keyTransportAlgorithm));
        }
        return new XmlObjectToBase64EncodedStringTransformer<>().apply(responseBuilder.build());
    }
}
