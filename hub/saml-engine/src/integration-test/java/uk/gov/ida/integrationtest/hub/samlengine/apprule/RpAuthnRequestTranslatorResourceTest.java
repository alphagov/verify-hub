package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.SamlRequestWithAuthnRequestInformationDto;
import uk.gov.ida.hub.samlengine.contracts.TranslatedAuthnRequestDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.test.AuthnRequestIdGenerator;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlengine.builders.SamlAuthnRequestDtoBuilder.aSamlAuthnRequest;
import static uk.gov.ida.hub.samlengine.builders.TranslatedAuthnRequestDtoBuilder.aTranslatedAuthnRequest;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP_MS;

public class RpAuthnRequestTranslatorResourceTest {

    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            ConfigOverride.config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration).build(RpAuthnRequestTranslatorResourceTest.class.getSimpleName());
    }

    @Before
    public void beforeEach() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP, TEST_RP_PUBLIC_SIGNING_CERT, TEST_RP_PUBLIC_ENCRYPTION_CERT);
        configStub.setUpStubForMatchingServiceDetails(TEST_RP_MS);
        configStub.setUpStubForRPMetadataEnabled(TEST_RP_MS);
        configStub.setUpStubForRPMetadataEnabled(TEST_RP);
    }

    @After
    public void tearDown() throws Exception {
        configStub.reset();
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldTranslateSamlAuthnRequestMessage() throws Exception {
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

        Response response = post(requestDto, samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(TranslatedAuthnRequestDto.class)).isEqualToComparingFieldByField(expectedResult);
    }

    @Test
    public void shouldThrowExceptionWhenAuthnRequestIsSignedByNonExistentRP() throws Exception {
        final SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withPublicCert(STUB_IDP_PUBLIC_PRIMARY_CERT)
                .withPrivateKey(STUB_IDP_PUBLIC_PRIMARY_PRIVATE_KEY)
                .build();

        configStub.setupStubForNonExistentSigningCertificates("nonexistent-rp");

        Response response = post(requestDto, samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldThrowInvalidSamlExceptionWhenTheAuthnRequestIsInvalid() throws Exception {
        SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withPublicCert(TEST_RP_PUBLIC_SIGNING_CERT)
                .withPrivateKey(TEST_RP_PRIVATE_SIGNING_KEY)
                .buildInvalid();

        Response response = post(requestDto, samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    private Response post(SamlRequestWithAuthnRequestInformationDto requestDto, URI uri) {
        return client.target(uri)
                .request().post(Entity.entity(requestDto, MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void shouldThrowExceptionWhenTheRequestIdIsADuplicate() throws Exception {
        SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withId("_iamtheoneandonlytheresnootherrequestididratherbe")
                .withIssuer(TEST_RP)
                .withPublicCert(TEST_RP_PUBLIC_SIGNING_CERT)
                .withPrivateKey(TEST_RP_PRIVATE_SIGNING_KEY)
                .build();

        post(requestDto, samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE));
        Response response = post(requestDto, samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_DUPLICATE_REQUEST_ID);
    }

    @Test
    public void authenticationRequestPost_shouldThrowExceptionWhenIssueInstantTooOld() throws Exception {
        DateTimeFreezer.freezeTime();

        DateTime issueInstant = DateTime.now().minusMinutes(5).minusSeconds(1);

        SamlRequestWithAuthnRequestInformationDto requestDto = aSamlAuthnRequest()
                .withIssueInstant(issueInstant)
                .withIssuer(TEST_RP)
                .withPublicCert(TEST_RP_PUBLIC_SIGNING_CERT)
                .withPrivateKey(TEST_RP_PRIVATE_SIGNING_KEY)
                .build();

        Response response = post(requestDto, samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_RP_AUTHN_REQUEST_RESOURCE));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML_REQUEST_TOO_OLD);
    }
}
