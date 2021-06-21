package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA1;
import org.w3c.dom.Element;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlsoapproxy.SamlSoapProxyApplication;
import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.dto.AggregatedMatchingServicesHealthCheckResultDto;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.dto.MatchingServiceHealthCheckResultDto;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.EventSinkStubExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlEngineStubExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlSoapProxyAppExtension;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;
import uk.gov.ida.saml.msa.test.api.MsaTransformersFactory;
import uk.gov.ida.saml.msa.test.outbound.HealthCheckResponseFromMatchingService;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.core.Response;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceHealthCheckerResponseDtoBuilder.anInboundResponseFromMatchingServiceDto;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubExtension.msaStubExtension;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlEngineStubExtension.stackedSamlEngineStubRule;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

public class MatchingServiceHealthCheckIntegrationTests {

    private static ClientSupport client;

    private static final String msaEntityId = "http://dev-rp-ms.local/SAML2/MD";
    private static final String msaEntityId2 = "http://another-rp-ms.local/SAML2/MD";
    private static final String msaVersion = "66.6";
    private static final String msaVersion2 = "33.3";
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA1();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();

    private static final SoapMessageManager soapMessageManager = new SoapMessageManager();

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(0)
    @RegisterExtension
    public static EventSinkStubExtension eventSinkStub = new EventSinkStubExtension();

    @Order(0)
    @RegisterExtension
    public static SamlEngineStubExtension samlEngineStub = stackedSamlEngineStubRule();

    @Order(0)
    @RegisterExtension
    public static MsaStubExtension msaStub1 = msaStubExtension();

    @Order(0)
    @RegisterExtension
    public static MsaStubExtension msaStub2 = msaStubExtension();

    @Order(1)
    @RegisterExtension
    public static TestDropwizardAppExtension samlSoapProxyAppExtension = SamlSoapProxyAppExtension.forApp(SamlSoapProxyApplication.class)
            .withDefaultConfigOverridesAnd()
            .configOverride("configUri", () -> configStub.baseUri().build().toASCIIString())
            .configOverride("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            .configOverride("samlEngineUri", () -> samlEngineStub.baseUri().build().toASCIIString())
            .config(ResourceHelpers.resourceFilePath("saml-soap-proxy.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) throws JsonProcessingException {
        client = clientSupport;
        eventSinkStub.setupStubForLogging();
        configStub.setupStubForCertificates(msaEntityId, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        configStub.setupStubForCertificates(msaEntityId2, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
    }

    @AfterAll
    public static void tearDown() {
        SamlSoapProxyAppExtension.tearDown();
    }
    
    @Test
    public void healthCheck_shouldRespondWith200WhenAllMatchingServicesHealthy() throws Exception {
        configStub.setUpStubForMatchingServiceHealthCheckRequest(msaStub1.getAttributeQueryRequestUri(), msaEntityId2);
        String healthCheckResponse = aHealthyHealthCheckResponse(msaEntityId2, msaVersion2);
        msaStub1.prepareForHealthCheckRequest(healthCheckResponse);
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
    }

    @Test
    public void healthCheck_shouldRespondWith500WhenAllMatchingServicesAreNotHealthy() throws Exception {
        configStub.setUpStubForTwoMatchingServiceHealthCheckRequests(msaStub1.getAttributeQueryRequestUri(), msaEntityId,
                msaStub2.getAttributeQueryRequestUri(), msaEntityId2);
        
        msaStub1.prepareForHealthCheckRequest(anUnhealthyHealthCheckResponse(msaEntityId, msaVersion));
        samlEngineStub.prepareForHealthCheckSamlGeneration();
        samlEngineStub.setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(),
                ExceptionType.INVALID_SAML));

        msaStub2.prepareForHealthCheckRequest(aHealthyHealthCheckResponse(msaEntityId2, msaVersion2));
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
        configStub.setUpStubForTwoMatchingServiceHealthCheckRequests(msaStub1.getAttributeQueryRequestUri(), msaEntityId,
                msaStub2.getAttributeQueryRequestUri(), msaEntityId2);

        msaStub1.prepareForHealthCheckRequest(anUnhealthyHealthCheckResponse(msaEntityId, msaVersion));
        samlEngineStub.prepareForHealthCheckSamlGeneration();
        samlEngineStub.setupStubForAttributeResponseTranslateReturningError(ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(),
                ExceptionType.INVALID_SAML));

        msaStub2.prepareForHealthCheckRequest(aHealthyHealthCheckResponse(msaEntityId2, msaVersion2));
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
        return client.targetMain(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_HEALTH_CHECK_RESOURCE).request().get();
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
