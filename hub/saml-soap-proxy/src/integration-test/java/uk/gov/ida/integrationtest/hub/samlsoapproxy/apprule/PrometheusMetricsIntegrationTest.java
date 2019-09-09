package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule;

import com.fasterxml.jackson.core.JsonProcessingException;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensaml.xmlsec.algorithm.DigestAlgorithm;
import org.opensaml.xmlsec.algorithm.SignatureAlgorithm;
import org.opensaml.xmlsec.algorithm.descriptors.DigestSHA256;
import org.opensaml.xmlsec.algorithm.descriptors.SignatureRSASHA256;
import org.w3c.dom.Element;
import uk.gov.ida.common.shared.security.PrivateKeyFactory;
import uk.gov.ida.common.shared.security.PublicKeyFactory;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlsoapproxy.contract.SamlMessageDto;
import uk.gov.ida.hub.samlsoapproxy.soap.SoapMessageManager;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.ConfigStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.EventSinkStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MatchingServiceDetails;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlEngineStubRule;
import uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlSoapProxyAppRule;
import uk.gov.ida.saml.msa.test.api.MsaTransformersFactory;
import uk.gov.ida.saml.msa.test.outbound.HealthCheckResponseFromMatchingService;
import uk.gov.ida.saml.security.IdaKeyStore;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.glassfish.jersey.internal.util.Base64.encodeAsString;
import static uk.gov.ida.hub.samlsoapproxy.builders.MatchingServiceHealthCheckerResponseDtoBuilder.anInboundResponseFromMatchingServiceDto;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_HELP;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_HEALTH_STATUS_LAST_UPDATED_HELP;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_INFO;
import static uk.gov.ida.hub.samlsoapproxy.client.PrometheusClient.VERIFY_SAML_SOAP_PROXY_MSA_INFO_HELP;
import static uk.gov.ida.hub.samlsoapproxy.service.MatchingServiceHealthCheckTask.HEALTHY;
import static uk.gov.ida.hub.samlsoapproxy.service.MatchingServiceHealthCheckTask.UNHEALTHY;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubRule.msaStubRule;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.MsaStubRule.sleepyMsaStubRule;
import static uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support.SamlEngineStubRule.samlEngineStubRule;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_ENCRYPTION_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PRIVATE_SIGNING_KEY;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus.Healthy;

public class PrometheusMetricsIntegrationTest {
    private static Client client;
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
    private static final String MSA_ONE_VERSION = "3.1.0-840";
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

    @ClassRule
    public static ConfigStubRule configStub = new ConfigStubRule();

    @ClassRule
    public static EventSinkStubRule eventSinkStub = new EventSinkStubRule();

    @ClassRule
    public static SamlEngineStubRule samlEngineStub = samlEngineStubRule();

    @ClassRule
    public static MsaStubRule msaStubOne = msaStubRule();

    @ClassRule
    public static MsaStubRule msaStubTwo = msaStubRule();

    @ClassRule
    public static MsaStubRule msaStubThree = msaStubRule();

    @ClassRule
    public static MsaStubRule msaStubFour = sleepyMsaStubRule(5_000);

    @ClassRule
    public static SamlSoapProxyAppRule samlSoapProxyAppRule = new SamlSoapProxyAppRule(
        config("logging.level", "INFO"),
        config("httpClient.gzipEnabledForRequests", "false"),
        config("matchingServiceHealthCheckServiceConfiguration.enable", "true"),
        config("matchingServiceHealthCheckServiceConfiguration.initialDelay", "1s"),
        config("matchingServiceHealthCheckServiceConfiguration.delay", "3s"),
        config("configUri", configStub.baseUri().build().toASCIIString()),
        config("eventSinkUri", eventSinkStub.baseUri().build().toASCIIString()),
        config("samlEngineUri", samlEngineStub.baseUri().build().toASCIIString())
    );

    @BeforeClass
    public static void setUpBeforeClass() {
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration()
                                                                                              .withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(samlSoapProxyAppRule.getEnvironment()).using(jerseyClientConfiguration)
                                                                               .build(PrometheusMetricsIntegrationTest.class.getSimpleName());
    }

    @Before
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
        final SamlMessageDto msaOneSamlMessage = new SamlMessageDto(encodeAsString(XmlUtils.writeToString(msaOneResponse)));
        final SamlMessageDto msaTwoSamlMessage = new SamlMessageDto(encodeAsString(XmlUtils.writeToString(msaTwoResponse)));
        final SamlMessageDto msaFourSamlMessage = new SamlMessageDto(encodeAsString(XmlUtils.writeToString(msaFourResponse)));
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
                                                                 final MsaStubRule msa) {
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
                                                                         final MsaStubRule msa,
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
        return client.target(UriBuilder.fromUri("http://localhost")
                                       .path("/prometheus/metrics")
                                       .port(samlSoapProxyAppRule.getAdminPort())
                                       .build())
                     .request()
                     .get();
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
