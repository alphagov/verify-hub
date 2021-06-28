package uk.gov.ida.integrationtest.hub.config.apprule;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;

import io.dropwizard.testing.ResourceHelpers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.EnableHook;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.data.S3ConfigSource;
import uk.gov.ida.hub.config.dto.CertificateDto;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

public class SelfServiceCertificatesResourceIntegrationTest {
    public static ClientSupport client;
    private static final String LOCAL_ONLY_ENTITY_ID = "https://msa.local.test.com";
    private static final String REMOTE_ENABLED_ENTITY_ID = "https://msa.bananaregistry.test.com";
    private static final String REMOTE_CERT = "MIIDFDCCAfwCCQDEj/3MbRb8jzANBgkqhkiG9w0BAQsFADBMMQswCQYDVQQGEwJVSzEPMA0GA1UEBwwGTG9uZG9uMQwwCgYDVQQKDANHRFMxHjAcBgNVBAMMFUJhbmFuYSBNU0EgRW5jcnlwdGlvbjAeFw0xOTA2MjgxNDI0MzFaFw0zOTA2MjgxNDI0MzFaMEwxCzAJBgNVBAYTAlVLMQ8wDQYDVQQHDAZMb25kb24xDDAKBgNVBAoMA0dEUzEeMBwGA1UEAwwVQmFuYW5hIE1TQSBFbmNyeXB0aW9uMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1FuGXjWgeNNJ7a5todg/+gQO+xKUf6/tJ0wIW4dHS1lEOnk3mWu5fCtyTbDG9O+O22EOqmxMzsF6Kl/U6qRhmqs8bmc5pW9AQ67JMlMYCmrLq/VhF2aQ9rZV/Dx9rd2xuU6IkJPWWryY6qFfNrh6CDHzFzM5y+iGAXNLj1Z0TY8J38hjgRWCjSq9XD8tYW3SFfsonMRm71CvLGNl0WQu3WEGMu4yDqQjH8QT7LF1IF3obSeUPJKDnVIKa5/7THu/Lrekon8LJ5BbBYeBvahqpbQbvf2UK+lEvgCOUupGoPjz6mQ97tjHXCtE04xMyDEkMFy2urnNv2e2eVuy0VHE4wIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQCacP1D5haveoTdwhxnWnCwgc6TFiMd4g5Io31bXOvwShmqcYoJ7t9gdD7ZiPMJPbcF/YGCCk/BSEUtvYOPaRJV7C3BIZEPnewoQXyhX1uKzSqsYFIssl7DyUuItnmLZCQ4+OHpp1JMprDaWoF5hk2TdgqSv/fNlxt0193ayLzV+Dt34LhaS/pwXEBG/WtmJW3fygEOnmqmL4SMfG6nvvd/pOxAUeMEnzct3lJ5j2Qv/c0k43fUsy267gIRz/dpB/zlEzA6uUnrCNVdz+1AVjzvo9kf7H/4cA348mnBnh/USbRoIXhPkbPp5GuD3Q2CHvAL+bqVcQVNAJr6HKl+OwC4";
    private static final String BUCKET_NAME = "s3bucket";
    private static final String OBJECT_KEY = "src/test/resources/remote-test-config.json";

    @RegisterExtension
    static TestDropwizardAppExtension app = ConfigAppExtension.forApp(ConfigApplication.class)
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
            .writeFederationConfig()
            .withClearedCollectorRegistry()
            .withDefaultConfigOverridesAnd(
                    "selfService.enabled: true",
                    "selfService.s3BucketName: " + BUCKET_NAME,
                    "selfService.s3ObjectKey: " + OBJECT_KEY,
                    "selfService.cacheExpiry: 5s")
            .config(ResourceHelpers.resourceFilePath("config.yml"))
            .randomPorts()
            .create();

    @EnableHook
    static GuiceyConfigurationHook HOOK = builder -> builder.modulesOverride(bindS3ConfigSource());

    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder().silent().withSecureConnection(false).build();

    @BeforeAll
    public static void setUp(ClientSupport clientSupport) throws Exception {
        client = clientSupport;
        AmazonS3 s3Client = S3_MOCK.createS3Client();
        s3Client.createBucket(BUCKET_NAME);
        s3Client.putObject(BUCKET_NAME, OBJECT_KEY, getTestJson("/remote-test-config.json"));
    }

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
        return client.targetMain(uri.toString()).request().buildGet().invoke();
    }

    private static Module bindS3ConfigSource() {
        return new AbstractModule() {
            @Override
            protected void configure() {
            }

            @Provides
            @SuppressWarnings("unused")
            private S3ConfigSource getS3ConfigSource(ConfigConfiguration configConfiguration, ObjectMapper objectMapper){
                SelfServiceConfig selfServiceConfig = configConfiguration.getSelfService();
                if (selfServiceConfig.isEnabled()){
                    AmazonS3 amazonS3 = S3_MOCK.createS3Client();
                    return new S3ConfigSource(
                            selfServiceConfig,
                            amazonS3,
                            objectMapper);
                }
                return new S3ConfigSource();
            }
        };
    }
}
