package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import org.w3c.dom.Element;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.dto.AggregatedMatchingServicesHealthCheckResultDto;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.dto.MatchingServiceHealthCheckResultDto;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlSoapProxyAppRule;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.msa.test.api.MsaTransformersFactory;
import uk.gov.ida.saml.msa.test.outbound.HealthCheckResponseFromMatchingService;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.xml.namespace.NamespaceContext;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceHealthCheckerResponseDtoBuilder.anInboundResponseFromMatchingServiceDto;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubRule.msaStubRule;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlEngineStubRule.stackedSamlEngineStubRule;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class MatchingServiceHealthCheckIntegrationTests {

    private static Client client;

    private static final String msaEntityId = "http://dev-rp-ms.local/SAML2/MD";
    private static final String msaEntityId2 = "http://another-rp-ms.local/SAML2/MD";
    private static final String msaVersion = "66.6";
    private static final String msaVersion2 = "33.3";
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA1();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();

    private static final SoapMessageManager soapMessageManager = new SoapMessageManager();

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = stackedSamlEngineStubRule();

    @ClassRule
    public static MsaStubRule msaStubRule1 = msaStubRule();

    @ClassRule
    public static MsaStubRule msaStubRule2 = msaStubRule();

    @ClassRule
    public static SamlSoapProxyAppRule samlSoapProxyAppRule = new SamlSoapProxyAppRule(
            config("configUri", configStub.baseUri().build().toASCIIString()),
            config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()),
            config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUp() throws Exception {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration()
                .withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlSoapProxyAppRule.getEnvironment()).using(jerseyClientConfiguration)
                .build(MatchingServiceHealthCheckIntegrationTests.class.getSimpleName());
        eventSinkStub.setupStubForLogging();
        configStub.setupStubForCertificates(msaEntityId, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        configStub.setupStubForCertificates(msaEntityId2, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
    }

    @After
    public void tearDown() throws Exception {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void healthCheck_shouldRespondWith200WhenAllMatchingServicesHealthy() throws Exception {
        configStub.setUpStubForMatchingServiceHealthCheckRequest(msaStubRule1.getAttributeQueryRequestUri(), msaEntityId2);
        String healthCheckResponse = aHealthyHealthCheckResponse(msaEntityId2, msaVersion2);
        msaStubRule1.prepareForHealthCheckRequest(healthCheckResponse);
        samlEngineStub.prepareForHealthCheckSamlGeneration();
        samlEngineStub.setupStubForAttributeResponseTranslate(anInboundResponseFromMatchingServiceDto()
                .withIssuer(msaEntityId2)
                .withStatus(MatchingServiceIdaStatus.Healthy)
                .build());

        final Response response = makeMatchingServiceHealthCheckRequest();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AggregatedMatchingServicesHealthCheckResultDto result = response.readEntity(AggregatedMatchingServicesHealthCheckResultDto.class);
        assertThat(result.isHealthy()).isTrue();
        final MatchingServiceHealthCheckResultDto matchingServiceHealthCheckResult = result.getResults().get(0);
        assertThat(matchingServiceHealthCheckResult.isHealthy()).isTrue();
        assertThat(matchingServiceHealthCheckResult.getDetails().getVersionNumber()).isEqualTo(msaVersion2);

        final String xPathExpression = "/saml2p:AttributeQuery/@ID";
        hasXPath(xPathExpression, namespaceContextForSaml(), is(msaEntityId2));
    }

    private NamespaceContext namespaceContextForSaml() {
        NamespaceContextImpl context = new NamespaceContextImpl();
        context.startPrefixMapping("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        context.startPrefixMapping("saml2p", "urn:oasis:names:tc:SAML:2.0:protocol");
        context.startPrefixMapping("saml2", "urn:oasis:names:tc:SAML:2.0:assertion");
        context.startPrefixMapping("ds", "http://www.w3.org/2000/09/xmldsig#");
        return context;
    }


    @Test
    public void healthCheck_shouldRespondWith500WhenAllMatchingServicesAreNotHealthy() throws Exception {
        configStub.setUpStubForTwoMatchingServiceHealthCheckRequests(msaStubRule1.getAttributeQueryRequestUri(), msaEntityId,
                msaStubRule2.getAttributeQueryRequestUri(), msaEntityId2);
        
        msaStubRule1.prepareForHealthCheckRequest(anUnhealthyHealthCheckResponse(msaEntityId, msaVersion));
        samlEngineStub.prepareForHealthCheckSamlGeneration();
        samlEngineStub.setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(),
                ExceptionType.INVALID_SAML));

        msaStubRule2.prepareForHealthCheckRequest(aHealthyHealthCheckResponse(msaEntityId2, msaVersion2));
        samlEngineStub.prepareForHealthCheckSamlGeneration();
        samlEngineStub.setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(),
                ExceptionType.INVALID_SAML));

        final Response response = makeMatchingServiceHealthCheckRequest();

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        AggregatedMatchingServicesHealthCheckResultDto result = response.readEntity(AggregatedMatchingServicesHealthCheckResultDto.class);
        assertThat(result.isHealthy()).isFalse();

        final MatchingServiceHealthCheckResultDto matchingServiceHealthCheckResult = result.getResults().get(0);
        assertThat(matchingServiceHealthCheckResult.isHealthy()).isFalse();
        assertThat(matchingServiceHealthCheckResult.getDetails().getVersionNumber()).isEqualTo(msaVersion);

        final MatchingServiceHealthCheckResultDto matchingServiceHealthCheckResult2 = result.getResults().get(1);
        assertThat(matchingServiceHealthCheckResult2.isHealthy()).isFalse();
        assertThat(matchingServiceHealthCheckResult2.getDetails().getVersionNumber()).isEqualTo(msaVersion2);
    }

    @Test
    public void healthCheck_shouldRespondWith200WhenAnyMatchingServicesHealthy() throws Exception {
        configStub.setUpStubForTwoMatchingServiceHealthCheckRequests(msaStubRule1.getAttributeQueryRequestUri(), msaEntityId,
                msaStubRule2.getAttributeQueryRequestUri(), msaEntityId2);

        msaStubRule1.prepareForHealthCheckRequest(anUnhealthyHealthCheckResponse(msaEntityId, msaVersion));
        samlEngineStub.prepareForHealthCheckSamlGeneration();
        samlEngineStub.setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(),
                ExceptionType.INVALID_SAML));

        msaStubRule2.prepareForHealthCheckRequest(aHealthyHealthCheckResponse(msaEntityId2, msaVersion2));
        samlEngineStub.prepareForHealthCheckSamlGeneration();
        samlEngineStub.setupStubForAttributeResponseTranslate(anInboundResponseFromMatchingServiceDto()
                .withIssuer(msaEntityId2)
                .withStatus(MatchingServiceIdaStatus.Healthy)
                .build());

        final Response response = makeMatchingServiceHealthCheckRequest();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AggregatedMatchingServicesHealthCheckResultDto result = response.readEntity(AggregatedMatchingServicesHealthCheckResultDto.class);
        assertThat(result.isHealthy()).isTrue();
        final MatchingServiceHealthCheckResultDto matchingServiceHealthCheckResult = result.getResults().get(0);
        assertThat(matchingServiceHealthCheckResult.isHealthy()).isFalse();
        assertThat(matchingServiceHealthCheckResult.getDetails().getVersionNumber()).isEqualTo(msaVersion);
        final MatchingServiceHealthCheckResultDto matchingServiceHealthCheckResult2 = result.getResults().get(1);
        assertThat(matchingServiceHealthCheckResult2.isHealthy()).isTrue();
        assertThat(matchingServiceHealthCheckResult2.getDetails().getVersionNumber()).isEqualTo(msaVersion2);

    }

    private String anUnhealthyHealthCheckResponse(String msaEntityId, String msaVersion) {
        Function<HealthCheckResponseFromMatchingService, Element> transformer = new MsaTransformersFactory().getHealthcheckResponseFromMatchingServiceToElementTransformer(
                null,
                getKeyStore(),
                s -> "who-knows",
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM
        );
        final HealthCheckResponseFromMatchingService healthCheckResponse = new HealthCheckResponseFromMatchingService(
                "response-id-123-version-"+msaVersion,
                msaEntityId,
                "request-id"
        );
        final Element healthyResponse = transformer.apply(healthCheckResponse);
        return XmlUtils.writeToString(soapMessageManager.wrapWithSoapEnvelope(healthyResponse));
    }

    private String aHealthyHealthCheckResponse(String msaEntityId, String msaVersion) {
        Function<uk.gov.ida.saml.msa.test.outbound.HealthCheckResponseFromMatchingService, Element> transformer = new MsaTransformersFactory().getHealthcheckResponseFromMatchingServiceToElementTransformer(
                null,
                getKeyStore(),
                s -> HUB_ENTITY_ID,
                SIGNATURE_ALGORITHM,
                DIGEST_ALGORITHM
        );
        final HealthCheckResponseFromMatchingService healthCheckResponse = new HealthCheckResponseFromMatchingService(
                "response-id-123-version-"+msaVersion,
                msaEntityId,
                "request-id"
        );
        final Element healthyResponse = transformer.apply(healthCheckResponse);
        return XmlUtils.writeToString(soapMessageManager.wrapWithSoapEnvelope(healthyResponse));
    }

    private Response makeMatchingServiceHealthCheckRequest() {
        return client.target(samlSoapProxyAppRule.getUri(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_HEALTH_CHECK_RESOURCE))
                .request()
                .get();
    }

    private IdaKeyStore getKeyStore() {
        List<KeyPair> encryptionKeyPairs = new ArrayList<>();
        PublicKeyFactory publicKeyFactory = new PublicKeyFactory(new X509CertificateFactory());
        PrivateKeyFactory privateKeyFactory = new PrivateKeyFactory();
        PublicKey encryptionPublicKey = publicKeyFactory.createPublicKey(HUB_TEST_PUBLIC_ENCRYPTION_CERT);
        PrivateKey encryptionPrivateKey = privateKeyFactory.createPrivateKey(Base64.getDecoder().decode(HUB_TEST_PRIVATE_ENCRYPTION_KEY.getBytes()));
        encryptionKeyPairs.add(new KeyPair(encryptionPublicKey, encryptionPrivateKey));
        PublicKey publicSigningKey = publicKeyFactory.createPublicKey(HUB_TEST_PUBLIC_SIGNING_CERT);
        PrivateKey privateSigningKey = privateKeyFactory.createPrivateKey(Base64.getDecoder().decode(HUB_TEST_PRIVATE_SIGNING_KEY.getBytes()));
        KeyPair signingKeyPair = new KeyPair(publicSigningKey, privateSigningKey);

        return new IdaKeyStore(signingKeyPair, encryptionKeyPairs);
    }
}
