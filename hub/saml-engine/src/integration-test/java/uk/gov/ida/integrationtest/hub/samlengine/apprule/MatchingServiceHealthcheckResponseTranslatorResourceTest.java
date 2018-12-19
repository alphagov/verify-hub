package uk.gov.ida.integrationtest.hub.samlengine.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
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
import uk.gov.ida.hub.samlengine.contracts.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlengine.apprule.support.SamlEngineAppRule;
import uk.gov.ida.saml.core.domain.SamlStatusCode;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.security.EncryptionCredentialFactory;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static io.dropwizard.testing.ConfigOverride.config;
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
    private static Client client;

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static SamlEngineAppRule samlEngineAppRule = new SamlEngineAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setup() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder
                .aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlEngineAppRule.getEnvironment()).using(jerseyClientConfiguration)
                .build(MatchingServiceHealthcheckResponseTranslatorResourceTest.class.getSimpleName());
    }

    @Before
    public void beforeEach() throws Exception {
        configStub.setupCertificatesForEntity(TEST_RP_MS, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        configStub.setUpStubForMatchingServiceDetails(TEST_RP_MS);
        configStub.setUpStubForRPMetadataEnabled(TEST_RP_MS);
    }

    @After
    public void tearDown() {
        configStub.reset();
    }

    @Test
    public void should_translateHealthcheckAttributeQueryResponse() throws Exception {
        final String msaStatusCode = SamlStatusCode.HEALTHY;
        final Status status = aStatus().withStatusCode(aStatusCode().withSubStatusCode(aStatusCode().withValue(msaStatusCode).build()).withValue(StatusCode.SUCCESS).build()).build();
        final String requestId = "requestId";

        final String saml = aValidMatchResponseFromMatchingService(requestId, status, DateTime.now().plusHours(1));
        Response response = postResponseForTranslation(new SamlMessageDto(Base64.encodeAsString(saml)));
        MatchingServiceHealthCheckerResponseDto entity = response.readEntity(MatchingServiceHealthCheckerResponseDto.class);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(entity.getStatus()).isEqualTo(MatchingServiceIdaStatus.Healthy);
        assertThat(entity.getInResponseTo()).isEqualTo(requestId);
        assertThat(entity.getIssuer()).isEqualTo(TEST_RP_MS);
    }

    @Test
    public void should_shouldReturnErrorStatusDtoWhenThereIsAProblem() throws Exception {
        Response response = postResponseForTranslation(new SamlMessageDto(Base64.encodeAsString("<saml/>")));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        ErrorStatusDto entity = response.readEntity(ErrorStatusDto.class);
        assertThat(entity.getExceptionType()).isEqualTo(ExceptionType.INVALID_SAML);
    }

    private Response postResponseForTranslation(SamlMessageDto dto) {
        final URI uri = samlEngineAppRule.getUri(Urls.SamlEngineUrls.TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE);
        return client.target(uri)
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
                                .buildWithEncrypterCredential(new EncryptionCredentialFactory(entityId -> {
                                    PublicKeyFactory keyFactory = new PublicKeyFactory(new X509CertificateFactory());
                                    return keyFactory.createPublicKey(HUB_TEST_PUBLIC_ENCRYPTION_CERT);
                                }).getEncryptingCredential(HUB_ENTITY_ID))
                ).build().getDOM());
    }

}
