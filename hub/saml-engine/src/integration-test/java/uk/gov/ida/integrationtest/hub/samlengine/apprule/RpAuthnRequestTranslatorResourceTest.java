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
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlRequestWithAuthnRequestInformationDto;
import uk.gov.ida.hub.samlengine.contracts.TranslatedAuthnRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppExtension;
import uk.gov.ida.saml.core.test.AuthnRequestIdGenerator;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlengine.builders.SamlAuthnRequestDtoBuilder.aSamlAuthnRequest;
import static uk.gov.ida.hub.samlengine.builders.TranslatedAuthnRequestDtoBuilder.aTranslatedAuthnRequest;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;

public class RpAuthnRequestTranslatorResourceTest {

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
    public void shouldTranslateSamlAuthnRequestMessage() {
        String id = AuthnRequestIdGenerator.generateRequestId();
        int assertionConsumerServiceIndex = 1;
        SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withId(id)
                .withIssuer(TEST_RP)
                .withForceAuthentication(false)
                .withAssertionConsumerIndex(assertionConsumerServiceIndex)
                .withPublicCert(TEST_RP_PUBLIC_SIGNING_CERT)
                .withPrivateKey(TEST_RP_PRIVATE_SIGNING_KEY)
                .build();

        TranslatedAuthnRequestDto expectedResult = aTranslatedAuthnRequest()
                .withId(id)
                .withIssuer(TEST_RP)
                .withForceAuthentication(false)
                .withAssertionConsumerServiceIndex(assertionConsumerServiceIndex)
                .build();

        Response response = post(requestDto, Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(TranslatedAuthnRequestDto.class)).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void shouldThrowExceptionWhenAuthnRequestIsSignedByNonExistentRP() {
        final SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withPublicCert(STUB_IDP_PUBLIC_PRIMARY_CERT)
                .withPrivateKey(STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY)
                .build();

        configStub.setupStubForNonExistentSigningCertificates("nonexistent-rp");

        Response response = post(requestDto, Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldThrowInvalidSamlExceptionWhenTheAuthnRequestIsInvalid() {
        SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withPublicCert(TEST_RP_PUBLIC_SIGNING_CERT)
                .withPrivateKey(TEST_RP_PRIVATE_SIGNING_KEY)
                .buildInvalid();

        Response response = post(requestDto, Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    private Response post(SamlRequestWithAuthnRequestInformationDto requestDto, String uri) {
        return client.targetMain(uri)
                .request().post(Entity.entity(requestDto, MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void shouldThrowExceptionWhenTheRequestIdIsADuplicate() {
        SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withId("_iamtheoneandonlytheresnootherrequestididratherbe")
                .withIssuer(TEST_RP)
                .withPublicCert(TEST_RP_PUBLIC_SIGNING_CERT)
                .withPrivateKey(TEST_RP_PRIVATE_SIGNING_KEY)
                .build();

        post(requestDto, Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE);
        Response response = post(requestDto, Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_DUPLICATE_REQUEST_ID);
    }

    @Test
    public void authenticationRequestPost_shouldThrowExceptionWhenIssueInstantTooOld() {
        DateTimeFreezer.freezeTime();

        DateTime issueInstant = DateTime.now().minusMinutes(5).minusSeconds(1);

        SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withIssueInstant(issueInstant)
                .withIssuer(TEST_RP)
                .withPublicCert(TEST_RP_PUBLIC_SIGNING_CERT)
                .withPrivateKey(TEST_RP_PRIVATE_SIGNING_KEY)
                .build();

        Response response = post(requestDto, Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_REQUEST_TOO_OLD);
    }
}
