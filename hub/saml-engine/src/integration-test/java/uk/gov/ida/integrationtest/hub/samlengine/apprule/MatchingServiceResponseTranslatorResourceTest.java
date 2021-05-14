package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.signature.support.SignatureException;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlengine.domain.SamlResponseContainerDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.security.KeyStoreBackedEncryptionCredentialResolver;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opensaml.saml.saml2.core.StatusCode.REQUESTER;
import static org.opensaml.saml.saml2.core.StatusCode.RESPONDER;
import static org.opensaml.saml.saml2.core.StatusCode.SUCCESS;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
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
    private static ClientSupport client;

    private Credential msaSigningCredential = new TestCredentialFactory(TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PRIVATE_SIGNING_KEY).getSigningCredential();

    private Credential invalidMsaSigningCredential = new TestCredentialFactory(
            TEST_RP_PUBLIC_SIGNING_CERT,
            TEST_RP_PRIVATE_SIGNING_KEY
    ).getSigningCredential();

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(1)
    @RegisterExtension
    public static TestDropwizardAppExtension samlEngineApp = SamlEngineAppExtension.forApp(SamlEngineApplication.class)
            .withDefaultConfigOverridesAnd()
            .configOverride("configUri", () -> configStub.baseUri().build().toASCIIString())
            .config(ResourceHelpers.resourceFilePath("saml-engine.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        configStub.setupCertificatesForEntity(TEST_RP, TEST_RP_PUBLIC_SIGNING_CERT, TEST_RP_PUBLIC_ENCRYPTION_CERT);
    }

    @AfterEach
    public void tearDown() throws Exception {
        configStub.reset();
        DateTimeFreezer.unfreezeTime();
    }

    @AfterAll
    public static void afterAll() {
        SamlEngineAppExtension.tearDown();
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_Match() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(SUCCESS).build()).build();
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(Base64.getEncoder().encodeToString(aValidMatchResponseFromMatchingService(requestId, status).getBytes()), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = clientResponse.readEntity(InboundResponseFromMatchingServiceDto.class);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(TEST_RP_MS);
        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(requestId);
        assertThat(inboundResponseFromMatchingServiceDto.getStatus().name()).isEqualTo(MatchingServiceIdaStatus.MatchingServiceMatch.name());
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().get()).isEqualTo(LevelOfAssurance.LEVEL_2);
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().isPresent()).isTrue();
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_NoMatch() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.NO_MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(RESPONDER).build()).build();
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(Base64.getEncoder().encodeToString(aValidNoMatchResponseFromMatchingService(requestId, status, TEST_RP_MS).getBytes()), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = clientResponse.readEntity(InboundResponseFromMatchingServiceDto.class);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(TEST_RP_MS);
        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(requestId);
        assertThat(inboundResponseFromMatchingServiceDto.getStatus().name()).isEqualTo(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService.name());
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance()).isNotPresent();
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion()).isNotPresent();
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_RequesterError() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = StatusCode.NO_AUTHN_CONTEXT;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(REQUESTER).build()).build();
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(Base64.getEncoder().encodeToString(aValidNoMatchResponseFromMatchingService(requestId, status, TEST_RP_MS).getBytes()), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = clientResponse.readEntity(InboundResponseFromMatchingServiceDto.class);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(TEST_RP_MS);
        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(requestId);
        assertThat(inboundResponseFromMatchingServiceDto.getStatus().name()).isEqualTo(MatchingServiceIdaStatus.RequesterError.name());
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance()).isNotPresent();
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion()).isNotPresent();
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_Created() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.CREATED;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(SUCCESS).build()).build();
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(Base64.getEncoder().encodeToString(aValidMatchResponseFromMatchingService(requestId, status).getBytes()), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = clientResponse.readEntity(InboundResponseFromMatchingServiceDto.class);
        assertThat(inboundResponseFromMatchingServiceDto.getIssuer()).isEqualTo(TEST_RP_MS);
        assertThat(inboundResponseFromMatchingServiceDto.getInResponseTo()).isEqualTo(requestId);
        assertThat(inboundResponseFromMatchingServiceDto.getStatus().name()).isEqualTo(MatchingServiceIdaStatus.UserAccountCreated.name());
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().isPresent()).isTrue();
        assertThat(inboundResponseFromMatchingServiceDto.getLevelOfAssurance().get()).isEqualTo(LevelOfAssurance.LEVEL_2);
        assertThat(inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().isPresent()).isTrue();
    }

    private Response postToSamlEngine(SamlResponseContainerDto samlResponseContainerDto) {
        return client
                    .targetMain(Urls.SamlEngineUrls.TRANSLATE_MATCHING_SERVICE_RESPONSE_RESOURCE)
                    .request()
                    .post(Entity.json(samlResponseContainerDto));
    }

    @Test
    public void shouldNotReturnADtoResponse_WhenFieldsAreMissing_Match() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(SUCCESS).build()).build();
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(Base64.getEncoder().encodeToString(aValidMatchResponseFromMatchingServiceWithMissingData(requestId, status, TEST_RP_MS).getBytes()), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);

    }

    @Test
    public void shouldNotReturnADtoResponse_WhenBadlySigned_NoMatch() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.NO_MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(RESPONDER).build()).build();
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(Base64.getEncoder().encodeToString(aValidNoMatchResponseFromMatchingServiceisBadlySigned(requestId, status, TEST_RP_MS).getBytes()), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldNotReturnADtoWhenResponseIs_bad() throws Exception {
        final String requestId = "requestId";
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(Base64.getEncoder().encodeToString(anInvalidAMatchingServiceSamlResponse(requestId).getBytes()), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldNotReturnADtoWhenResponseIs_Nonsense() {
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(StringUtils.rightPad("test", 2000, "x"), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

        assertThat(clientResponse.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto errorStatusDto = clientResponse.readEntity(ErrorStatusDto.class);
        assertThat(errorStatusDto.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    @Test
    public void shouldReturnADtoWhenResponseIs_TooOld() throws Exception {
        final String requestId = "requestId";
        final String msaStatusCode = SamlStatusCode.MATCH;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(SUCCESS).build()).build();
        final SamlResponseContainerDto samlResponseContainerDto = new SamlResponseContainerDto(Base64.getEncoder().encodeToString(aValidMatchResponseFromMatchingService(requestId, status, DateTime.now().minusDays(1)).getBytes()), TEST_RP);

        Response clientResponse = postToSamlEngine(samlResponseContainerDto);

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
                                .buildWithEncrypterCredential(new KeyStoreBackedEncryptionCredentialResolver(entityId -> {
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
                                .buildWithEncrypterCredential(new KeyStoreBackedEncryptionCredentialResolver(entityId -> {
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
