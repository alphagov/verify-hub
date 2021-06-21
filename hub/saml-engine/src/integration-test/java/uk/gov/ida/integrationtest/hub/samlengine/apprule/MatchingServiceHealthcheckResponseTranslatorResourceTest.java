package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import io.dropwizard.testing.ResourceHelpers;
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
import uk.gov.ida.hub.samlengine.contracts.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.security.KeyStoreBackedEncryptionCredentialResolver;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
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

public class MatchingServiceHealthcheckResponseTranslatorResourceTest {
    private static ClientSupport client;

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
    }

    @AfterEach
    public void tearDown() {
        configStub.reset();
    }

    @AfterAll
    public static void afterAll() {
        SamlEngineAppExtension.tearDown();
    }


    @Test
    public void should_translateHealthcheckAttributeQueryResponse() throws Exception {
        final String msaStatusCode = SamlStatusCode.HEALTHY;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(StatusCode.SUCCESS).build()).build();
        final String requestId = "requestId";

        final String saml = aValidMatchResponseFromMatchingService(requestId, status, DateTime.now().plusHours(1));
        Response response = postResponseForTranslation(new SamlMessageDto(Base64.getEncoder().encodeToString(saml.getBytes())));
        MatchingServiceHealthCheckerResponseDto entity = response.readEntity(MatchingServiceHealthCheckerResponseDto.class);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(entity.getStatus()).isEqualTo(MatchingServiceIdaStatus.Healthy);
        assertThat(entity.getInResponseTo()).isEqualTo(requestId);
        assertThat(entity.getIssuer()).isEqualTo(TEST_RP_MS);
    }

    @Test
    public void should_shouldReturnErrorStatusDtoWhenThereIsAProblem() {
        Response response = postResponseForTranslation(new SamlMessageDto(Base64.getEncoder().encodeToString("<saml/>".getBytes())));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    private Response postResponseForTranslation(SamlMessageDto dto) {
        return client.targetMain(Urls.SamlEngineUrls.TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(dto));

    }

    private String aValidMatchResponseFromMatchingService(final String requestId, final Status status, DateTime notOnOrAfter) throws MarshallingException, SignatureException {
        Credential signingCredential = new TestCredentialFactory(TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PRIVATE_SIGNING_KEY).getSigningCredential();
        return XmlUtils.writeToString(aResponse()
                .withStatus(status)
                .withInResponseTo(requestId)
                .withIssuer(anIssuer().withIssuerId(TEST_RP_MS).build())
                .withSigningCredential(signingCredential)
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
                                .withSignature(aSignature().withSigningCredential(signingCredential).build())
                                .addAuthnStatement(anAuthnStatement().build())
                                .buildWithEncrypterCredential(new KeyStoreBackedEncryptionCredentialResolver(entityId -> {
                                    PublicKeyFactory keyFactory = new PublicKeyFactory(new X509CertificateFactory());
                                    return keyFactory.createPublicKey(HUB_TEST_PUBLIC_ENCRYPTION_CERT);
                                }).getEncryptingCredential(HUB_ENTITY_ID))
                ).build().getDOM());
    }

}
