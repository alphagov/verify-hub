package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.testing.ResourceHelpers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.w3c.dom.Element;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlsoapproxy.SamlSoapProxyApplication;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlsoapproxy.contract.SamlMessageDto;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.ConfigStubExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.EventSinkStubExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MatchingServiceDetails;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlEngineStubExtension;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlSoapProxyAppExtension;
import uk.gov.ida.saml.msa.test.api.MsaTransformersFactory;
import uk.gov.ida.saml.msa.test.outbound.HealthCheckResponseFromMatchingService;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.core.Response;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceHealthCheckerResponseDtoBuilder.anInboundResponseFromMatchingServiceDto;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_HELP;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED_HELP;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_INFO;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_INFO_HELP;
import static uk.gov.ida.hub.samlsoapproxy.service.MatchingServiceHealthCheckTask.HEALTHY;
import static uk.gov.ida.hub.samlsoapproxy.service.MatchingServiceHealthCheckTask.UNHEALTHY;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubExtension.msaStubExtension;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubExtension.sleepyMsaStubExtension;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlEngineStubExtension.samlEngineStubRule;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus.Healthy;

public class PrometheusMetricsIntegrationTest {
    private static ClientSupport client;
    private static final int WAIT_FOR_MSA_HEALTH_METRICS_TO_BE_UPDATED = 5_000;
    private static final String GAUGE_HELP_TEMPLATE = "# HELP %s %s\n";
    private static final String GAUGE_TYPE_TEMPLATE = "# TYPE %s gauge\n";
    private static final String MSA_HEALTH_METRICS_TEMPLATE = "%s{matchingService=\"%s\",} %s\n";
    private static final String MSA_INFO_METRICS_TEMPLATE = "%s{matchingService=\"%s\",versionNumber=\"%s\",versionSupported=\"%s\",eidasEnabled=\"%s\",shouldSignWithSha1=\"%s\",onboarding=\"%s\",} %s\n";
    private static final String MSA_RESPONSE_ID_TEMPLATE = "healthcheck-response-id-version-%s-eidasenabled-%s-shouldsignwithsha1-%s";
    private static final String MSA_ONE_ENTITY_ID = "https://ms-one.local/SAML2/MD";
    private static final String MSA_TWO_ENTITY_ID = "https://ms-two.local/SAML2/MD";
    private static final String MSA_THREE_ENTITY_ID = "https://ms-three.local/SAML2/MD";
    private static final String MSA_FOUR_ENTITY_ID = "https://ms-four.local/SAML2/MD";
    private static final String MSA_ONE_VERSION = "5.1.0-5.1.0";
    private static final String MSA_TWO_VERSION = "33.3";
    private static final String MSA_FOUR_VERSION = "592";
    private static final String MSA_ONE_VERSION_SUPPORTED = "true";
    private static final String MSA_TWO_VERSION_SUPPORTED = "false";
    private static final String ONBOARDING = "false";
    private static final boolean MSA_ONE_EIDAS_ENABLED = true;
    private static final boolean MSA_TWO_EIDAS_ENABLED = false;
    private static final boolean MSA_FOUR_EIDAS_ENABLED = false;
    private static final boolean MSA_ONE_SHOULD_SIGN_WITH_SHA_1 = false;
    private static final boolean MSA_TWO_SHOULD_SIGN_WITH_SHA_1 = true;
    private static final boolean MSA_FOUR_SHOULD_SIGN_WITH_SHA_1 = false;
    private static final String MSA_ONE_RESPONSE_ID = String.format(MSA_RESPONSE_ID_TEMPLATE, MSA_ONE_VERSION, MSA_ONE_EIDAS_ENABLED, MSA_ONE_SHOULD_SIGN_WITH_SHA_1);
    private static final String MSA_TWO_RESPONSE_ID = String.format(MSA_RESPONSE_ID_TEMPLATE, MSA_TWO_VERSION, MSA_TWO_EIDAS_ENABLED, MSA_TWO_SHOULD_SIGN_WITH_SHA_1);
    private static final String MSA_FOUR_RESPONSE_ID = String.format(MSA_RESPONSE_ID_TEMPLATE, MSA_FOUR_VERSION, MSA_FOUR_EIDAS_ENABLED, MSA_FOUR_SHOULD_SIGN_WITH_SHA_1);
    private static final String RP_ONE_ENTITY_ID = "https://rp-one.local/SAML2/MD";
    private static final String RP_TWO_ENTITY_ID = "https://rp-two.local/SAML2/MD";
    private static final String RP_THREE_ENTITY_ID = "https://rp-three.local/SAML2/MD";
    private static final String RP_FOUR_ENTITY_ID = "https://rp-four.local/SAML2/MD";
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = new SignatureRSASHA256();
    private static final DigestAlgorithm DIGEST_ALGORITHM = new DigestSHA256();
    private static final SoapMessageManager SOAP_MESSAGE_MANAGER = new SoapMessageManager();

    @Order(0)
    @RegisterExtension
    public static ConfigStubExtension configStub = new ConfigStubExtension();

    @Order(0)
    @RegisterExtension
    public static EventSinkStubExtension eventSinkStub = new EventSinkStubExtension();

    @Order(0)
    @RegisterExtension
    public static SamlEngineStubExtension samlEngineStub = samlEngineStubRule();

    @Order(0)
    @RegisterExtension
    public static MsaStubExtension msaStubOne = msaStubExtension();

    @Order(0)
    @RegisterExtension
    public static MsaStubExtension msaStubTwo = msaStubExtension();

    @Order(0)
    @RegisterExtension
    public static MsaStubExtension msaStubThree = msaStubExtension();

    @Order(0)
    @RegisterExtension
    public static MsaStubExtension msaStubFour = sleepyMsaStubExtension(5_000);

    @Order(1)
    @RegisterExtension
    public static TestDropwizardAppExtension samlSoapProxyAppExtension = SamlSoapProxyAppExtension.forApp(SamlSoapProxyApplication.class)
            .withDefaultConfigOverridesAnd(
                    "logging.level: INFO",
                    "httpClient.gzipEnabledForRequests: false",
                    "matchingServiceHealthCheckServiceConfiguration.enable: true",
                    "matchingServiceHealthCheckServiceConfiguration.initialDelay: 1s",
                    "matchingServiceHealthCheckServiceConfiguration.delay: 3s"
            )
            .configOverride("configUri", () -> configStub.baseUri().build().toASCIIString())
            .configOverride("eventSinkUri", () -> eventSinkStub.baseUri().build().toASCIIString())
            .configOverride("samlEngineUri", () -> samlEngineStub.baseUri().build().toASCIIString())
            .config(ResourceHelpers.resourceFilePath("saml-soap-proxy.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    public static void beforeClass(ClientSupport clientSupport) {
        client = clientSupport;
    }

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        final MatchingServiceDetails msaOneDetails = new MatchingServiceDetails(
            msaStubOne.getAttributeQueryRequestUri(), MSA_ONE_ENTITY_ID, RP_ONE_ENTITY_ID);
        final MatchingServiceDetails msaTwoDetails = new MatchingServiceDetails(
            msaStubTwo.getAttributeQueryRequestUri(), MSA_TWO_ENTITY_ID, RP_TWO_ENTITY_ID);
        final MatchingServiceDetails msaThreeDetails = new MatchingServiceDetails(
            msaStubThree.getAttributeQueryRequestUri(), MSA_THREE_ENTITY_ID, RP_THREE_ENTITY_ID);
        final MatchingServiceDetails msaFourDetails = new MatchingServiceDetails(
            msaStubFour.getAttributeQueryRequestUri(), MSA_FOUR_ENTITY_ID, RP_FOUR_ENTITY_ID);
        Set<MatchingServiceDetails> msaDetailsSet = new HashSet<>(Set.of(msaOneDetails, msaTwoDetails, msaThreeDetails, msaFourDetails));
        final Element msaOneResponse = aHealthyHealthCheckResponse(MSA_ONE_ENTITY_ID, MSA_ONE_RESPONSE_ID, MSA_ONE_VERSION);
        final Element msaTwoResponse = aHealthyHealthCheckResponse(MSA_TWO_ENTITY_ID, MSA_TWO_RESPONSE_ID, MSA_TWO_VERSION);
        final Element msaFourResponse = aHealthyHealthCheckResponse(MSA_FOUR_ENTITY_ID, MSA_FOUR_RESPONSE_ID, MSA_FOUR_VERSION);
        final SamlMessageDto msaOneSamlMessage = new SamlMessageDto(Base64.getEncoder().encodeToString(XmlUtils.writeToString(msaOneResponse).getBytes()));
        final SamlMessageDto msaTwoSamlMessage = new SamlMessageDto(Base64.getEncoder().encodeToString(XmlUtils.writeToString(msaTwoResponse).getBytes()));
        final SamlMessageDto msaFourSamlMessage = new SamlMessageDto(Base64.getEncoder().encodeToString(XmlUtils.writeToString(msaFourResponse).getBytes()));
        final MatchingServiceHealthCheckerRequestDto msaOneHealthCheckerRequest = new MatchingServiceHealthCheckerRequestDto(RP_ONE_ENTITY_ID, MSA_ONE_ENTITY_ID);
        final MatchingServiceHealthCheckerRequestDto msaTwoHealthCheckerRequest = new MatchingServiceHealthCheckerRequestDto(RP_TWO_ENTITY_ID, MSA_TWO_ENTITY_ID);
        final MatchingServiceHealthCheckerRequestDto msaThreeHealthCheckerRequest = new MatchingServiceHealthCheckerRequestDto(RP_THREE_ENTITY_ID, MSA_THREE_ENTITY_ID);
        final MatchingServiceHealthCheckerRequestDto msaFourHealthCheckerRequest = new MatchingServiceHealthCheckerRequestDto(RP_FOUR_ENTITY_ID, MSA_FOUR_ENTITY_ID);

        eventSinkStub.setupStubForLogging();
        configStub.setUpStubForMatchingServiceHealthCheckRequests(msaDetailsSet);
        configStub.setupStubForCertificates(MSA_ONE_ENTITY_ID, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        configStub.setupStubForCertificates(MSA_TWO_ENTITY_ID, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        configStub.setupStubForCertificates(MSA_FOUR_ENTITY_ID, TEST_RP_MS_PUBLIC_SIGNING_CERT, TEST_RP_MS_PUBLIC_ENCRYPTION_CERT);
        msaStubOne.prepareForHealthCheckRequest(
            XmlUtils.writeToString(SOAP_MESSAGE_MANAGER.wrapWithSoapEnvelope(msaOneResponse)));
        msaStubTwo.prepareForHealthCheckRequest(
            XmlUtils.writeToString(SOAP_MESSAGE_MANAGER.wrapWithSoapEnvelope(msaTwoResponse)));
        msaStubFour.prepareForHealthCheckRequest(
            XmlUtils.writeToString(SOAP_MESSAGE_MANAGER.wrapWithSoapEnvelope(msaFourResponse)));
        samlEngineStub.prepareForHealthCheckSamlGeneration(msaOneHealthCheckerRequest);
        samlEngineStub.prepareForHealthCheckSamlGeneration(msaTwoHealthCheckerRequest);
        samlEngineStub.prepareForHealthCheckSamlGeneration(msaThreeHealthCheckerRequest);
        samlEngineStub.prepareForHealthCheckSamlGeneration(msaFourHealthCheckerRequest);
        samlEngineStub.setupStubForAttributeResponseTranslate(
            msaOneSamlMessage,
            anInboundResponseFromMatchingServiceDto().withIssuer(MSA_ONE_ENTITY_ID)
                                                     .withStatus(Healthy)
                                                     .build());
        samlEngineStub.setupStubForAttributeResponseTranslate(
            msaTwoSamlMessage,
            anInboundResponseFromMatchingServiceDto().withIssuer(MSA_TWO_ENTITY_ID)
                                                     .withStatus(Healthy)
                                                     .build());
        samlEngineStub.setupStubForAttributeResponseTranslate(
            msaFourSamlMessage,
            anInboundResponseFromMatchingServiceDto().withIssuer(MSA_FOUR_ENTITY_ID)
                                                     .withStatus(Healthy)
                                                     .build());
    }

    @Test
    public void shouldHaveUpdatedMsaHealthMetrics() throws InterruptedException {
        DateTimeFreezer.freezeTime();
        final DateTime firstTimestamp = DateTime.now(DateTimeZone.UTC);
        Thread.sleep(WAIT_FOR_MSA_HEALTH_METRICS_TO_BE_UPDATED);
        Response response = getPrometheusMetrics();

        assertThatMsasHealthMetricsAreCorrect(response);
        DateTimeFreezer.unfreezeTime();

        DateTimeFreezer.freezeTime();
        final DateTime secondTimestamp = DateTime.now(DateTimeZone.UTC);
        Thread.sleep(WAIT_FOR_MSA_HEALTH_METRICS_TO_BE_UPDATED);
        response = getPrometheusMetrics();

        assertThat(firstTimestamp).isNotEqualTo(secondTimestamp);
        assertThatMsasHealthMetricsAreCorrect(response);
        DateTimeFreezer.unfreezeTime();
    }

    private void assertThatMsasHealthMetricsAreCorrect(final Response response) {
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        final String entity = response.readEntity(String.class);
        assertThat(entity).contains(String.format(GAUGE_HELP_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_HELP));
        assertThat(entity).contains(String.format(GAUGE_TYPE_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS));
        assertThat(entity).contains(String.format(GAUGE_HELP_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED_HELP));
        assertThat(entity).contains(String.format(GAUGE_TYPE_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED));

        assertThat(entity).contains(String.format(GAUGE_HELP_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_INFO, VERIFY_SAML_SOAP_PROXY_MSA_INFO_HELP));
        assertThat(entity).contains(String.format(GAUGE_TYPE_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_INFO));

        assertThatAHealthyMsaHasBothCorrectHealthAndInfoMetrics(entity, msaStubOne, MSA_ONE_VERSION, MSA_ONE_VERSION_SUPPORTED, MSA_ONE_EIDAS_ENABLED, MSA_ONE_SHOULD_SIGN_WITH_SHA_1);
        assertThatAHealthyMsaHasBothCorrectHealthAndInfoMetrics(entity, msaStubTwo, MSA_TWO_VERSION, MSA_TWO_VERSION_SUPPORTED, MSA_TWO_EIDAS_ENABLED, MSA_TWO_SHOULD_SIGN_WITH_SHA_1);
        assertThatAnUnhealthyMsaHasACorrectHealthMetric(entity, msaStubThree);
        assertThatAnUnhealthyMsaHasACorrectHealthMetric(entity, msaStubFour);
    }

    private void assertThatAnUnhealthyMsaHasACorrectHealthMetric(final String entity,
                                                                 final MsaStubExtension msa) {
        assertThat(entity).contains(String.format(
            MSA_HEALTH_METRICS_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS,
            msa.getAttributeQueryRequestUri(),
            UNHEALTHY));
        assertThat(entity).contains(String.format(
            MSA_HEALTH_METRICS_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED,
            msa.getAttributeQueryRequestUri(),
            (double) DateTime.now(DateTimeZone.UTC).getMillis()));
        assertThat(entity).contains(String.format(VERIFY_SAML_SOAP_PROXY_MSA_INFO + "{matchingService=\"%s\",", msa.getAttributeQueryRequestUri()));
    }

    private void assertThatAHealthyMsaHasBothCorrectHealthAndInfoMetrics(final String entity,
                                                                         final MsaStubExtension msa,
                                                                         final String msaVersion,
                                                                         final String msaVersionSupported,
                                                                         final boolean msaEidasEnabled,
                                                                         final boolean msaShouldSignWithSha1) {
        assertThat(entity).contains(String.format(
            MSA_HEALTH_METRICS_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS,
            msa.getAttributeQueryRequestUri(),
            HEALTHY));
        assertThat(entity).contains(String.format(
            MSA_HEALTH_METRICS_TEMPLATE, VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED,
            msa.getAttributeQueryRequestUri(),
            (double) DateTime.now(DateTimeZone.UTC).getMillis()));
        assertThat(entity).contains(String.format(
            MSA_INFO_METRICS_TEMPLATE,
            VERIFY_SAML_SOAP_PROXY_MSA_INFO,
            msa.getAttributeQueryRequestUri(),
            msaVersion,
            msaVersionSupported,
            msaEidasEnabled,
            msaShouldSignWithSha1,
            ONBOARDING,
            HEALTHY));
    }

    private Response getPrometheusMetrics() {
        return client.targetAdmin("/prometheus/metrics").request().get();
    }

    private static Element aHealthyHealthCheckResponse(final String msaEntityId,
                                                       final String responseId, String msaVersion) {
        Function<HealthCheckResponseFromMatchingService, Element> transformer = new MsaTransformersFactory().getHealthcheckResponseFromMatchingServiceToElementTransformer(
            null,
            getKeyStore(),
            s -> HUB_ENTITY_ID,
            SIGNATURE_ALGORITHM,
            DIGEST_ALGORITHM);
        final HealthCheckResponseFromMatchingService healthCheckResponse = new HealthCheckResponseFromMatchingService(
            responseId,
            msaEntityId,
            "request-id-"+msaVersion
        );
        return transformer.apply(healthCheckResponse);
    }

    private static IdaKeyStore getKeyStore() {
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
