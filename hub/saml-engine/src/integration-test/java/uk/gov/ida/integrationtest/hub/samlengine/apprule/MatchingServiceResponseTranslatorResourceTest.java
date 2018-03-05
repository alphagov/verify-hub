package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.internal.util.Base64;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.domain.SamlResponseDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.security.EncryptionCredentialFactory;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.opensaml.saml.saml2.core.StatusCode.REQUESTER;
import static org.opensaml.saml.saml2.core.StatusCode.RESPONDER;
import static org.opensaml.saml.saml2.core.StatusCode.SUCCESS;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder.anAuthnStatement;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.SignatureBuilder.aSignature;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder.aSubjectConfirmationData;

public class MatchingServiceResponseTranslatorResourceTest {
    private static Client client;

    private Credential msaSigningCredential = new TestCredentialFactory(TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PRIVATE_SIGNING_KEY).getSigningCredential();

    private Credential invalidMsaSigningCredential = new TestCredentialFactory(
            TEST_RP_PUBLIC_SIGNING_CERT,
            TEST_RP_PRIVATE_SIGNING_KEY
    ).getSigningCredential();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();


    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(RpAuthnRequestTranslatorResourceTest.class.getSimpleName());
    }

    @Before
    public void beforeEach() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
    }

    @After
    public void tearDown() throws Exception {
        configStub.reset();
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_Match() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(SUCCESS).build()).build();
        final SamlResponseDto samlResponseDto = new SamlResponseDto(Base64.encodeAsString(aValidMatchResponseFromMatchingService(requestId, status)));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = clientResponse.readEntity(InboundResponseFromMatchingServiceDto.class);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(TEST_RP_MS);
        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(requestId);
        assertThat(inboundResponseFromMatchingServiceDto.getStatus().name()).isEqualTo(MatchingServiceIdaStatus.MatchingServiceMatch.name());
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().get()).isEqualTo(LevelOfAssurance.LEVEL_2);
        assertThat(inboundResponseFromMatchingServiceDto.getUnderlyingMatchingServiceAssertionBlob().isPresent()).isTrue();
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_NoMatch() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.NO_MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(RESPONDER).build()).build();
        final SamlResponseDto samlResponseDto = new SamlResponseDto(Base64.encodeAsString(aValidNoMatchResponseFromMatchingService(requestId, status, TEST_RP_MS)));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = clientResponse.readEntity(InboundResponseFromMatchingServiceDto.class);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(TEST_RP_MS);
        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(requestId);
        assertThat(inboundResponseFromMatchingServiceDto.getStatus().name()).isEqualTo(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService.name());
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isFalse();
        assertThat(inboundResponseFromMatchingServiceDto.getUnderlyingMatchingServiceAssertionBlob().isPresent()).isFalse();
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_RequesterError() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = StatusCode.NO_AUTHN_CONTEXT;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(REQUESTER).build()).build();
        final SamlResponseDto samlResponseDto = new SamlResponseDto(Base64.encodeAsString(aValidNoMatchResponseFromMatchingService(requestId, status, TEST_RP_MS)));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = clientResponse.readEntity(InboundResponseFromMatchingServiceDto.class);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(TEST_RP_MS);
        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(requestId);
        assertThat(inboundResponseFromMatchingServiceDto.getStatus().name()).isEqualTo(MatchingServiceIdaStatus.RequesterError.name());
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isFalse();
        assertThat(inboundResponseFromMatchingServiceDto.getUnderlyingMatchingServiceAssertionBlob().isPresent()).isFalse();
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_Created() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.CREATED;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(SUCCESS).build()).build();
        final SamlResponseDto samlResponseDto = new SamlResponseDto(Base64.encodeAsString(aValidMatchResponseFromMatchingService(requestId, status)));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = clientResponse.readEntity(InboundResponseFromMatchingServiceDto.class);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(TEST_RP_MS);
        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(requestId);
        assertThat(inboundResponseFromMatchingServiceDto.getStatus().name()).isEqualTo(MatchingServiceIdaStatus.UserAccountCreated.name());
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().get()).isEqualTo(LevelOfAssurance.LEVEL_2);
        assertThat(inboundResponseFromMatchingServiceDto.getUnderlyingMatchingServiceAssertionBlob().isPresent()).isTrue();
    }

    private Response postToSamlEngine(SamlResponseDto samlResponseDto) {
        return client
                    .target(samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_MATCHING_SERVICE_RESPONSE_RESOURCE))
                    .request()
                    .post(Entity.json(samlResponseDto));
    }

    @Test
    public void shouldNotReturnADtoResponse_WhenFieldsAreMissing_Match() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(SUCCESS).build()).build();
        final SamlResponseDto samlResponseDto = new SamlResponseDto(Base64.encodeAsString(aValidMatchResponseFromMatchingServiceWithMissingData(requestId, status, TEST_RP_MS)));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);

    }

    @Test
    public void shouldNotReturnADtoResponse_WhenBadlySigned_NoMatch() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.NO_MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(RESPONDER).build()).build();
        final SamlResponseDto samlResponseDto = new SamlResponseDto(Base64.encodeAsString(aValidNoMatchResponseFromMatchingServiceisBadlySigned(requestId, status, TEST_RP_MS)));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldNotReturnADtoWhenResponseIs_bad() throws Exception {
        final String requestId = "requestId";
        final SamlResponseDto samlResponseDto = new SamlResponseDto(Base64.encodeAsString(anInvalidAMatchingServiceSamlResponse(requestId)));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldNotReturnADtoWhenResponseIs_Nonsense() {
        final SamlResponseDto samlResponseDto = new SamlResponseDto(StringUtils.rightPad("test", 2000, "x"));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_TooOld() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(SUCCESS).build()).build();
        final SamlResponseDto samlResponseDto = new SamlResponseDto(Base64.encodeAsString(aValidMatchResponseFromMatchingService(requestId, status, DateTime.now().minusDays(1))));

        Response clientResponse = postToSamlEngine(samlResponseDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    private String anInvalidAMatchingServiceSamlResponse(String requestId) throws Exception {
        return XmlUtils.writeToString(aResponse()
                .withInResponseTo(requestId)
                .withIssuer(anIssuer().withIssuerId(TEST_RP_MS).build())
                .withSigningCredential(invalidMsaSigningCredential)
                .withStatus(aStatus().withStatusCode(aStatusCode().withValue(SUCCESS).build()).build())
                .build().getDOM());
    }

    private String aValidMatchResponseFromMatchingService(final String requestId, final Status status, DateTime notOnOrAfter) throws MarshallingException, SignatureException {
        return XmlUtils.writeToString(aResponse()
                .withStatus(status)
                .withInResponseTo(requestId)
                .withIssuer(anIssuer().withIssuerId(TEST_RP_MS).build())
                .withSigningCredential(msaSigningCredential)
                .addEncryptedAssertion(
                        anAssertion()
                                .withSubject(
                                        aSubject()
                                                .withSubjectConfirmation(
                                                        aSubjectConfirmation()
                                                                .withSubjectConfirmationData(
                                                                        aSubjectConfirmationData()
                                                                                .withInResponseTo(requestId)
                                                                                .withNotOnOrAfter(
                                                                                        notOnOrAfter
                                                                                )
                                                                                .build()
                                                                )
                                                                .build()
                                                )
                                                .build()
                                )
                                .withIssuer(anIssuer().withIssuerId(TEST_RP_MS).build())
                                .withSignature(
                                        aSignature().withSigningCredential(msaSigningCredential).build()
                                )
                                .addAuthnStatement(anAuthnStatement().build())
                                .buildWithEncrypterCredential(new EncryptionCredentialFactory(entityId -> {
                                    PublicKeyFactory keyFactory = new PublicKeyFactory(new X509CertificateFactory());
                                    return keyFactory.createPublicKey(HUB_TEST_PUBLIC_ENCRYPTION_CERT);
                                }).getEncryptingCredential(HUB_ENTITY_ID))
                ).build().getDOM());
    }

    private String aValidMatchResponseFromMatchingService(final String requestId, final Status status) throws MarshallingException, SignatureException {
        return aValidMatchResponseFromMatchingService(requestId, status, DateTime.now().plusDays(5));
    }

    private String aValidMatchResponseFromMatchingServiceWithMissingData(final String requestId, final Status status, String msaEntityId) throws MarshallingException, SignatureException {
        return XmlUtils.writeToString(aResponse()
                .withStatus(status)
                .withInResponseTo(requestId)
                .withIssuer(anIssuer().withIssuerId(msaEntityId).build())
                .withSigningCredential(msaSigningCredential)
                .addEncryptedAssertion(
                        anAssertion()
                                .withSubject(
                                        aSubject()
                                                .withSubjectConfirmation(
                                                        aSubjectConfirmation()
                                                                .withSubjectConfirmationData(
                                                                        aSubjectConfirmationData()
                                                                                .withInResponseTo(requestId)
                                                                                .withNotOnOrAfter(
                                                                                        DateTime.now()
                                                                                                .plusDays(5)
                                                                                )
                                                                                .build()
                                                                )
                                                                .build()
                                                )
                                                .build()
                                )
                                .withIssuer(anIssuer().withIssuerId(msaEntityId).build())
                                .withSignature(
                                        aSignature().withSigningCredential(msaSigningCredential).build()
                                )
                                .buildWithEncrypterCredential(new EncryptionCredentialFactory(entityId -> {
                                    PublicKeyFactory keyFactory = new PublicKeyFactory(new X509CertificateFactory());
                                    return keyFactory.createPublicKey(HUB_TEST_PUBLIC_ENCRYPTION_CERT);
                                }).getEncryptingCredential(HUB_ENTITY_ID))
                ).build().getDOM());
    }

    private String aValidNoMatchResponseFromMatchingServiceisBadlySigned(final String requestId, final Status status, String msaEntityId) throws MarshallingException, SignatureException {
        return XmlUtils.writeToString(aResponse()
                .withStatus(status)
                .withInResponseTo(requestId)
                .withIssuer(anIssuer().withIssuerId(msaEntityId).build())
                .withSigningCredential(invalidMsaSigningCredential)
                .withNoDefaultAssertion()
                .build().getDOM());
    }

    private String aValidNoMatchResponseFromMatchingService(final String requestId, final Status status, String msaEntityId) throws MarshallingException, SignatureException {
        return XmlUtils.writeToString(aResponse()
                .withStatus(status)
                .withInResponseTo(requestId)
                .withIssuer(anIssuer().withIssuerId(msaEntityId).build())
                .withSigningCredential(msaSigningCredential)
                .withNoDefaultAssertion()
                .build().getDOM());
    }

}
