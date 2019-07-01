package uk.gov.ida.integrationtest.hub.config.apprule;

import com.adobe.testing.s3mock.S3MockRule;
import com.amazonaws.services.s3.AmazonS3;
import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.util.Duration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.dto.CertificateDto;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppRule;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;


public class SelfServiceCertificatesResourceIntegrationTest {
    public static Client client;

    private static final String LOCAL_ONLY_ENTITY_ID = "https://msa.local.test.com";
    private static final String REMOTE_ENABLED_ENTITY_ID = "https://msa.bananaregistry.test.com";
    private static final String REMOTE_CERT_REGEX = "MIIDFDCCAfwCCQDEj/3MbRb8jzANBgkqhkiG9w0BAQsFADBMMQswCQYDVQQGEwJVSzEPMA0GA1UEBwwGTG9uZG9uMQwwCgYDVQQKDANHRFMxHjAcBgNVBAMMFUJhbmFuYSBNU0EgRW5jcnlwdGlvbjAeFw0xOTA2MjgxNDI0MzFaFw0zOTA2MjgxNDI0MzFaMEwxCzAJBgNVBAYTAlVLMQ8wDQYDVQQHDAZMb25kb24xDDAKBgNVBAoMA0dEUzEeMBwGA1UEAwwVQmFuYW5hIE1TQSBFbmNyeXB0aW9uMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1FuGXjWgeNNJ7a5todg/\\+gQO\\+xKUf6/tJ0wIW4dHS1lEOnk3mWu5fCtyTbDG9O\\+O22EOqmxMzsF6Kl/U6qRhmqs8bmc5pW9AQ67JMlMYCmrLq/VhF2aQ9rZV/Dx9rd2xuU6IkJPWWryY6qFfNrh6CDHzFzM5y\\+iGAXNLj1Z0TY8J38hjgRWCjSq9XD8tYW3SFfsonMRm71CvLGNl0WQu3WEGMu4yDqQjH8QT7LF1IF3obSeUPJKDnVIKa5/7THu/Lrekon8LJ5BbBYeBvahqpbQbvf2UK\\+lEvgCOUupGoPjz6mQ97tjHXCtE04xMyDEkMFy2urnNv2e2eVuy0VHE4wIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCacP1D5haveoTdwhxnWnCwgc6TFiMd4g5Io31bXOvwShmqcYoJ7t9gdD7ZiPMJPbcF/YGCCk/BSEUtvYOPaRJV7C3BIZEPnewoQXyhX1uKzSqsYFIssl7DyUuItnmLZCQ4\\+OHpp1JMprDaWoF5hk2TdgqSv/fNlxt0193ayLzV\\+Dt34LhaS/pwXEBG/WtmJW3fygEOnmqmL4SMfG6nvvd/pOxAUeMEnzct3lJ5j2Qv/c0k43fUsy267gIRz/dpB/zlEzA6uUnrCNVdz\\+1AVjzvo9kf7H/4cA348mnBnh/USbRoIXhPkbPp5GuD3Q2CHvAL\\+bqVcQVNAJr6HKl\\+OwC4";
    private static final String REMOTE_CERT_SUB = TEST_RP_MS_PUBLIC_SIGNING_CERT.replaceAll("\n","");
    private static final String BUCKET_NAME = "s3bucket";
    private static final String OBJECT_KEY = "src/test/resources/remote-test-config.json";


    public static S3MockRule s3MockRule = new S3MockRule();

    public static ConfigAppRule configAppRule = new ConfigAppRule(s3MockRule::createS3Client, getSelfServiceOverrides())
            .addTransaction(aTransactionConfigData()
                    .withEntityId("rp-entity-id")
                    .withMatchingServiceEntityId(LOCAL_ONLY_ENTITY_ID)
                    .build())
            .addTransaction(aTransactionConfigData()
                    .withEntityId("rp-entity-id-no-matching")
                    .withUsingMatching(false)
                    .build())
            .addMatchingService(aMatchingServiceConfig()
                    .withEntityId(LOCAL_ONLY_ENTITY_ID)
                    .build())
            .addMatchingService(aMatchingServiceConfig()
                    .withEntityId(REMOTE_ENABLED_ENTITY_ID)
                    .withSelfService(true)
                    .build())
            .addIdp(anIdentityProviderConfigData()
                    .withEntityId("idp-entity-id")
                    .withOnboarding(asList("rp-entity-id"))
                    .build());

    @ClassRule
    public static RuleChain ruleChain = RuleChain.outerRule(s3MockRule)
            .around(configAppRule);

    private static ConfigOverride[] getSelfServiceOverrides() {
        ConfigOverride[] overrides = {
                ConfigOverride.config("selfService.enabled", "true"),
                ConfigOverride.config("selfService.s3BucketName", BUCKET_NAME),
                ConfigOverride.config("selfService.s3ObjectKey", OBJECT_KEY),
                ConfigOverride.config("selfService.cacheExpiry", "5s")
        };
        return overrides;
    }


    @BeforeClass
    public static void setUp() throws Exception {
        AmazonS3 s3Client = s3MockRule.createS3Client();
        s3Client.createBucket(BUCKET_NAME);
        s3Client.putObject(BUCKET_NAME, OBJECT_KEY, substituteCerts("/remote-test-config.json", REMOTE_CERT_SUB));
        JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(configAppRule.getEnvironment()).using(jerseyClientConfiguration).build(SelfServiceCertificatesResourceIntegrationTest.class.getSimpleName());
    }


    private static String substituteCerts(String resource, String remoteCertSub) throws URISyntaxException, IOException {
        URI uri = SelfServiceCertificatesResourceIntegrationTest.class.getResource(resource).toURI();
        String original = new String(Files.readAllBytes(Path.of(uri)));
        return original.replaceAll(REMOTE_CERT_REGEX, remoteCertSub);
    }

    @Test
    public void getEncryptionCertificateReturnsOkAndEncryptionCertificateForLocalOnly(){
        String entityId = LOCAL_ONLY_ENTITY_ID;
        Response response = getForEntityIdAndPath(entityId, Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE);
        assertForEntityId(entityId, response);
    }

    @Test
    public void getEncryptionCertificateReturnsOkAndEncryptionCertificateForOverridden(){
        String entityId = REMOTE_ENABLED_ENTITY_ID;
        Response response = getForEntityIdAndPath(entityId, Urls.ConfigUrls.ENCRYPTION_CERTIFICATES_RESOURCE);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        CertificateDto certDto = response.readEntity(CertificateDto.class);
        assertThat(certDto.getCertificate()).contains(REMOTE_CERT_SUB);
    }

    private void assertForEntityId(String entityId, Response response){
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(CertificateDto.class).getIssuerId()).isEqualTo(entityId);
    }
    private Response getForEntityIdAndPath(String entityId, String path) {
        URI uri = configAppRule.getUri(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        return client.target(uri).request().get();
    }
}
