package uk.gov.ida.integrationtest.hub.config.apprule;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.dto.CertificateDto;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension.ConfigClient;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension.ConfigAppExtensionBuilder;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigIntegrationApplication;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

public class SelfServiceCertificatesResourceIntegrationTest {
    private static final String LOCAL_ONLY_ENTITY_ID = "https://msa.local.test.com";
    private static final String REMOTE_ENABLED_ENTITY_ID = "https://msa.bananaregistry.test.com";
    private static final String REMOTE_CERT = "MIIDFDCCAfwCCQDEj/3MbRb8jzANBgkqhkiG9w0BAQsFADBMMQswCQYDVQQGEwJVSzEPMA0GA1UEBwwGTG9uZG9uMQwwCgYDVQQKDANHRFMxHjAcBgNVBAMMFUJhbmFuYSBNU0EgRW5jcnlwdGlvbjAeFw0xOTA2MjgxNDI0MzFaFw0zOTA2MjgxNDI0MzFaMEwxCzAJBgNVBAYTAlVLMQ8wDQYDVQQHDAZMb25kb24xDDAKBgNVBAoMA0dEUzEeMBwGA1UEAwwVQmFuYW5hIE1TQSBFbmNyeXB0aW9uMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1FuGXjWgeNNJ7a5todg/+gQO+xKUf6/tJ0wIW4dHS1lEOnk3mWu5fCtyTbDG9O+O22EOqmxMzsF6Kl/U6qRhmqs8bmc5pW9AQ67JMlMYCmrLq/VhF2aQ9rZV/Dx9rd2xuU6IkJPWWryY6qFfNrh6CDHzFzM5y+iGAXNLj1Z0TY8J38hjgRWCjSq9XD8tYW3SFfsonMRm71CvLGNl0WQu3WEGMu4yDqQjH8QT7LF1IF3obSeUPJKDnVIKa5/7THu/Lrekon8LJ5BbBYeBvahqpbQbvf2UK+lEvgCOUupGoPjz6mQ97tjHXCtE04xMyDEkMFy2urnNv2e2eVuy0VHE4wIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCacP1D5haveoTdwhxnWnCwgc6TFiMd4g5Io31bXOvwShmqcYoJ7t9gdD7ZiPMJPbcF/YGCCk/BSEUtvYOPaRJV7C3BIZEPnewoQXyhX1uKzSqsYFIssl7DyUuItnmLZCQ4+OHpp1JMprDaWoF5hk2TdgqSv/fNlxt0193ayLzV+Dt34LhaS/pwXEBG/WtmJW3fygEOnmqmL4SMfG6nvvd/pOxAUeMEnzct3lJ5j2Qv/c0k43fUsy267gIRz/dpB/zlEzA6uUnrCNVdz+1AVjzvo9kf7H/4cA348mnBnh/USbRoIXhPkbPp5GuD3Q2CHvAL+bqVcQVNAJr6HKl+OwC4";
    private static final String BUCKET_NAME = "s3bucket";
    private static final String OBJECT_KEY = "src/test/resources/remote-test-config.json";

    @Order(0)
    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder().silent().withSecureConnection(false).build();

    @Order(1)
    @RegisterExtension
    static final DropwizardExtensionsSupport dropwizardExtensionSupport = new DropwizardExtensionsSupport();

    private static final ConfigAppExtension app = ConfigAppExtensionBuilder.forApp(ConfigIntegrationApplication.class)
            .withS3ClientSupplier(S3_MOCK::createS3Client)
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
                    .withOnboarding(singletonList("rp-entity-id"))
                    .build())
            .withConfigOverrides(
                    config("selfService.enabled", "true"),
                    config("selfService.s3BucketName", BUCKET_NAME),
                    config("selfService.s3ObjectKey", OBJECT_KEY),
                    config("selfService.cacheExpiry", "5s"))
            .build();

    private ConfigClient client;

    @BeforeEach
    void setup() { client = app.getClient(); }

    @BeforeAll
    public static void setUp() throws Exception {
        AmazonS3 s3Client = S3_MOCK.createS3Client();
        s3Client.createBucket(BUCKET_NAME);
        s3Client.putObject(BUCKET_NAME, OBJECT_KEY, getTestJson("/remote-test-config.json"));
    }

    @AfterAll
    static void tearDown() { app.tearDown(); }

    private static String getTestJson(String resource) throws URISyntaxException, IOException {
        URI uri = SelfServiceCertificatesResourceIntegrationTest.class.getResource(resource).toURI();
        return new String(Files.readAllBytes(Path.of(uri)));
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
        assertThat(certDto.getCertificate()).contains(REMOTE_CERT);
    }

    private void assertForEntityId(String entityId, Response response){
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.readEntity(CertificateDto.class).getIssuerId()).isEqualTo(entityId);
    }

    private Response getForEntityIdAndPath(String entityId, String path) {
        URI uri = UriBuilder.fromPath(path).buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        return client.targetMain(uri);
    }
}
