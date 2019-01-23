package uk.gov.ida.integrationtest.hub.config.apprule;

import helpers.JerseyClientConfigurationBuilder;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import uk.gov.ida.hub.config.CertificateEntity;
import uk.gov.ida.hub.config.ConfigEntityData;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppRule;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;

public class PrometheusMetricsIntegrationTest {
    private static Client client;
    private static final String VERIFY_CONFIG_CERTIFICATE_EXPIRY = "verify_config_certificate_expiry";
    private static final String VERIFY_CONFIG_CERTIFICATE_OCSP_SUCCESS = "verify_config_certificate_ocsp_success";
    private static final String RP_ENTITY_ID = "rp-entity-id";
    private static final String RP_MS_ENTITY_ID = "rp-ms-entity-id";
    private static final String CERTIFICATE_METRICS_TEMPLATE = "%s{entity_id=\"%s\",use=\"%s\",subject=\"%s\",fingerprint=\"%s\",} %s\n";
    private static final TransactionConfigEntityData TRANSACTION_CONFIG_ENTITY_DATA = aTransactionConfigData().withEntityId(RP_ENTITY_ID).withMatchingServiceEntityId(RP_MS_ENTITY_ID).build();
    private static final MatchingServiceConfigEntityData MATCHING_SERVICE_CONFIG_ENTITY_DATA = aMatchingServiceConfigEntityData().withEntityId(RP_MS_ENTITY_ID).build();
    private static final IdentityProviderConfigEntityData IDENTITY_PROVIDER_CONFIG_ENTITY_DATA = anIdentityProviderConfigData().withEntityId("idp-entity-id").withOnboarding(asList(RP_ENTITY_ID)).build();

    @ClassRule
    public static ConfigAppRule configAppRule = new ConfigAppRule(config("prometheusEnabled", "true"))
                                                    .addTransaction(TRANSACTION_CONFIG_ENTITY_DATA)
                                                    .addMatchingService(MATCHING_SERVICE_CONFIG_ENTITY_DATA)
                                                    .addIdp(IDENTITY_PROVIDER_CONFIG_ENTITY_DATA);

    @BeforeClass
    public static void setUp() {
        final JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(configAppRule.getEnvironment()).using(jerseyClientConfiguration).build(CertificatesResourceIntegrationTest.class.getSimpleName());
    }

    @Test
    public void shouldHaveCertificatesMetrics() {
        final Response response = client.target(UriBuilder.fromUri("http://localhost")
                                                          .path("/prometheus/metrics")
                                                          .port(configAppRule.getAdminPort())
                                                          .build())
                                        .request()
                                        .get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        final String entity = response.readEntity(String.class);
        assertThat(entity).contains("# TYPE " + VERIFY_CONFIG_CERTIFICATE_EXPIRY + " gauge\n");
        assertThat(entity).contains("# TYPE " + VERIFY_CONFIG_CERTIFICATE_OCSP_SUCCESS + " gauge\n");
        getExpectedCertificatesMetrics().forEach(expectedCertificateMetric -> assertThat(entity).contains(expectedCertificateMetric));
    }

    private List<String> getExpectedCertificatesMetrics() {
        List<String> expectedCertificatesMetrics = new ArrayList<>();
        expectedCertificatesMetrics.addAll(getExpectedCertificateMetricsList(TRANSACTION_CONFIG_ENTITY_DATA));
        expectedCertificatesMetrics.addAll(getExpectedCertificateMetricsList(MATCHING_SERVICE_CONFIG_ENTITY_DATA));
        return expectedCertificatesMetrics;
    }

    private <T extends ConfigEntityData & CertificateEntity> List<String> getExpectedCertificateMetricsList(final T configEntityData) {
        List<String> expectedCertificatesMetrics = new ArrayList<>();
        configEntityData.getSignatureVerificationCertificates()
                        .forEach(
                            certificate -> {
                                getExpectedCertificateExpiryDateMetrics(configEntityData.getEntityId(), certificate).ifPresent(
                                    expectedCertificateExpiryMetrics -> expectedCertificatesMetrics.add(expectedCertificateExpiryMetrics));
                                getExpectedCertificateOcspRevocationStatusMetrics(configEntityData.getEntityId(), certificate).ifPresent(
                                    expectedCertificateOcspRevocationStatusMetrics -> expectedCertificatesMetrics.add(expectedCertificateOcspRevocationStatusMetrics));
                            });
        getExpectedCertificateExpiryDateMetrics(configEntityData.getEntityId(), configEntityData.getEncryptionCertificate()).ifPresent(
            expectedCertificateExpiryMetrics -> expectedCertificatesMetrics.add(expectedCertificateExpiryMetrics));
        getExpectedCertificateOcspRevocationStatusMetrics(configEntityData.getEntityId(), configEntityData.getEncryptionCertificate()).ifPresent(
            expectedCertificateOcspRevocationStatusMetrics -> expectedCertificatesMetrics.add(expectedCertificateOcspRevocationStatusMetrics));
        return expectedCertificatesMetrics;
    }

    private Optional<String> getExpectedCertificateExpiryDateMetrics(final String entityId,
                                                                     final Certificate certificate) {
        try {
            return Optional.of(String.format(CERTIFICATE_METRICS_TEMPLATE,
                VERIFY_CONFIG_CERTIFICATE_EXPIRY,
                entityId,
                certificate.getCertificateType(),
                certificate.getSubject(),
                certificate.getFingerprint(),
                new Double(new DateTime(certificate.getNotAfter().getTime(), DateTimeZone.UTC).getMillis())));
        } catch (CertificateException e) {
            System.err.println(String.format("Unable to get NotAfter from the certificate [issuer = %s, type = %s]", entityId, certificate.getCertificateType()));
        }
        return Optional.empty();
    }

    private Optional<String> getExpectedCertificateOcspRevocationStatusMetrics(final String entityId,
                                                                               final Certificate certificate) {
        try {
            return Optional.of(String.format(CERTIFICATE_METRICS_TEMPLATE,
                VERIFY_CONFIG_CERTIFICATE_OCSP_SUCCESS,
                entityId,
                certificate.getCertificateType(),
                certificate.getSubject(),
                certificate.getFingerprint(),
                new Double(0.0)));
        } catch (CertificateException e) {
            System.err.println(String.format("Unable to get NotAfter from the certificate [issuer = %s, type = %s]", entityId, certificate.getCertificateType()));
        }
        return Optional.empty();
    }
}
