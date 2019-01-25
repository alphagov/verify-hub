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
import uk.gov.ida.hub.config.domain.EncryptionCertificate;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppRule;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

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
import static uk.gov.ida.hub.config.application.OcspCertificateChainValidationService.INVALID;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_EXPIRY;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_OCSP_SUCCESS;
import static uk.gov.ida.hub.config.domain.builders.EncryptionCertificateBuilder.anEncryptionCertificate;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder.aSignatureVerificationCertificate;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;

public class PrometheusMetricsIntegrationTest {
    private static Client client;
    private static final int WAIT_FOR_CERTIFICATE_METRICS_TO_BE_UPDATED = 2_500;
    private static final String CERTIFICATE_EXCEPTION_MESSAGE = "Unable to get NotAfter from the certificate [issuer = %s, type = %s]";
    private static final String RP_ENTITY_ID = "rp-entity-id";
    private static final String RP_MS_ENTITY_ID = "rp-ms-entity-id";
    private static final String CERTIFICATE_METRICS_TEMPLATE = "%s{entity_id=\"%s\",use=\"%s\",subject=\"%s\",fingerprint=\"%s\",timestamp=\"%s\",} %s\n";
    private static final TransactionConfigEntityData TRANSACTION_CONFIG_ENTITY_DATA = aTransactionConfigData().withEntityId(RP_ENTITY_ID)
                                                                                                              .withMatchingServiceEntityId(RP_MS_ENTITY_ID)
                                                                                                              .build();
    private static final MatchingServiceConfigEntityData MATCHING_SERVICE_CONFIG_ENTITY_DATA = aMatchingServiceConfigEntityData().withEntityId(RP_MS_ENTITY_ID)
                                                                                                                                 .build();
    private static final IdentityProviderConfigEntityData IDENTITY_PROVIDER_CONFIG_ENTITY_DATA = anIdentityProviderConfigData().withEntityId("idp-entity-id")
                                                                                                                               .withOnboarding(asList(RP_ENTITY_ID))
                                                                                                                               .build();
    private static final String RP_ENTITY_ID_BAD_SIGNATURE_CERT = "rp-entity-id-bad-cert";
    private static final String RP_ENTITY_ID_BAD_ENCRYPTION_CERT = "rp-entity-id-bad-encryption-cert";
    private static final String BAD_CERTIFICATE_VALUE = "MIIEZzCCA0+gAwIBAgIQX/UeEoUFa9978uQ8FbLFyDANBgkqhkiG9w0BAQsFADBZMQswCQYDVQQGEwJHQjEXMBUGA1UEChMOQ2FiaW5ldCBPZmZpY2UxDDAKBgNVBAsTA0dEUzEjMCEGA1UEAxMaSURBUCBSZWx5aW5nIFBhcnR5IFRlc3QgQ0EwHhcNMTUwODI3MDAwMDAwWhcNMTcwODI2MjM1OTU5WjCBgzELMAkGA1UEBhMCR0IxDzANBgNVBAgTBkxvbmRvbjEPMA0GA1UEBxMGTG9uZG9uMRcwFQYDVQQKFA5DYWJpbmV0IE9mZmljZTEMMAoGA1UECxQDR0RTMSswKQYDVQQDEyJTYW1wbGUgUlAgU2lnbmluZyAoMjAxNTA4MjYxNjMzMDcpMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuIdy6fiwdlLpMOsOiZC8DXcAU1eKDKz0w04TRAdUMR4rdv36IcyTfUortDHQ60pmX4I/s5iksey4UHCqTNZKpw6coCboyFGtGy1M6tTFhrxKc/pZmjEqV0kqgfjUnVWqiOnjpuWOJsCRfScjGfJ4Gio0omnrfX6KOTrnieaSM7aZJ7WkWUe4KRGOyxBywRIyFFbUeNgIbD/IfV7GFZCLUa9XwKjnaidTTmEhihC0TiBcnl3NCeqSwNK0TsIYSh/k5i7U/QeIvc6w34lacHOsqL5woRMPBnmS91brY/hy/vdePx7Nk8Hiwx7VpLsn5b0BVJnEZcLs5gwDid0Vra+6kQIDAQABo4H/MIH8MAwGA1UdEwEB/wQCMAAwYQYDVR0fBFowWDBWoFSgUoZQaHR0cDovL29uc2l0ZWNybC50cnVzdHdpc2UuY29tL0NhYmluZXRPZmZpY2VJREFQUmVseWluZ1BhcnR5VGVzdENBL0xhdGVzdENSTC5jcmwwDgYDVR0PAQH/BAQDAgeAMB0GA1UdDgQWBBSsYjo5j/oZAQ/h35orm1VR+n5hVTAfBgNVHSMEGDAWgBTd5PVdGgoPOtFIIh5OwPhuNvbFJTA5BggrBgEFBQcBAQQtMCswKQYIKwYBBQUHMAGGHWh0dHA6Ly9zdGQtb2NzcC50cnVzdHdpc2UuY29tMA0GCSqGSIb3DQEBCwUAA4IBAQBHYp/kWufCENWW8xI/rwVRJrOjvYxbhyEM61QoMZzTqfSQVuaBCv1qwXTMU8D+iPVtSVStFdU+vxWrU0z8ZQcd9107wZtnIJWwoJJ4WJlrmXTzBNvlqc8Q57G4Y/x9SZZdyVn4JrQRK8Vm5NzZqYZeXqgMk5xeQEObY8EQFmdryZeh/B2j0WFm3ywXOYcz77a1e1WCxBgOULPh1sQD793KjbJlEUfyeq5w/cIPovI8u4xXa78ionzq+L9t3oRh/wuTNjG/qezgArncr53sV2RZzb45RtT9+PxdQ1YFbQM7lL526kxVij0+FS6+b+EBx2CBVLWalmOugi0vA9vYpZJL";
    private static final SignatureVerificationCertificate BAD_SIGNATURE_CERTIFICATE = aSignatureVerificationCertificate().withX509(BAD_CERTIFICATE_VALUE)
                                                                                                                         .build();
    private static final EncryptionCertificate BAD_ENCRYPTION_CERTIFICATE = anEncryptionCertificate().withX509(BAD_CERTIFICATE_VALUE)
                                                                                                     .build();

    @ClassRule
    public static ConfigAppRule configAppRule = new ConfigAppRule(
        config("prometheusEnabled", "true"),
        config("certificateExpiryDateCheckServiceConfiguration.enable", "true"),
        config("certificateExpiryDateCheckServiceConfiguration.initialDelay", "1s"),
        config("certificateExpiryDateCheckServiceConfiguration.delay", "2s"),
        config("certificateOcspRevocationStatusCheckServiceConfiguration.enable", "true"),
        config("certificateOcspRevocationStatusCheckServiceConfiguration.initialDelay", "1s"),
        config("certificateOcspRevocationStatusCheckServiceConfiguration.delay", "2s"))
                                                        .addTransaction(TRANSACTION_CONFIG_ENTITY_DATA)
                                                        .addTransaction(aTransactionConfigData().withEntityId(RP_ENTITY_ID_BAD_ENCRYPTION_CERT)
                                                                                                .withMatchingServiceEntityId(RP_MS_ENTITY_ID)
                                                                                                .withEncryptionCertificate(BAD_ENCRYPTION_CERTIFICATE)
                                                                                                .build())
                                                        .addTransaction(aTransactionConfigData().withEntityId(RP_ENTITY_ID_BAD_SIGNATURE_CERT)
                                                                                                .withMatchingServiceEntityId(RP_MS_ENTITY_ID)
                                                                                                .addSignatureVerificationCertificate(BAD_SIGNATURE_CERTIFICATE)
                                                                                                .build())
                                                        .addMatchingService(MATCHING_SERVICE_CONFIG_ENTITY_DATA)
                                                        .addIdp(IDENTITY_PROVIDER_CONFIG_ENTITY_DATA);

    @BeforeClass
    public static void setUpBeforeClass() {
        final JerseyClientConfiguration jerseyClientConfiguration = JerseyClientConfigurationBuilder.aJerseyClientConfiguration().withTimeout(Duration.seconds(10)).build();
        client = new JerseyClientBuilder(configAppRule.getEnvironment()).using(jerseyClientConfiguration).build(PrometheusMetricsIntegrationTest.class.getSimpleName());
    }

    @Test
    public void shouldHaveUpdatedCertificatesMetrics() throws InterruptedException {
        DateTimeFreezer.freezeTime();
        Thread.sleep(WAIT_FOR_CERTIFICATE_METRICS_TO_BE_UPDATED);
        final DateTime firstTimestamp = DateTime.now(DateTimeZone.UTC);
        Response response = getPrometheusMetrics();

        assertThatCertificatesMetricsAreCorrect(response);
        DateTimeFreezer.unfreezeTime();

        DateTimeFreezer.freezeTime();
        Thread.sleep(WAIT_FOR_CERTIFICATE_METRICS_TO_BE_UPDATED);
        final DateTime secondTimestamp = DateTime.now(DateTimeZone.UTC);
        response = getPrometheusMetrics();

        assertThat(firstTimestamp).isNotEqualTo(secondTimestamp);
        assertThatCertificatesMetricsAreCorrect(response);
        DateTimeFreezer.unfreezeTime();
    }

    private void assertThatCertificatesMetricsAreCorrect(final Response response) {
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        final String entity = response.readEntity(String.class);
        assertThat(entity).contains(String.format("# TYPE %s gauge\n", VERIFY_CONFIG_CERTIFICATE_EXPIRY));
        assertThat(entity).contains(String.format("# TYPE %s gauge\n", VERIFY_CONFIG_CERTIFICATE_OCSP_SUCCESS));
        getExpectedCertificatesMetrics().forEach(expectedCertificateMetric -> assertThat(entity).contains(expectedCertificateMetric));
    }

    private Response getPrometheusMetrics() {
        return client.target(UriBuilder.fromUri("http://localhost").path("/prometheus/metrics").port(configAppRule.getAdminPort()).build()).request().get();
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
                Long.toString(DateTime.now(DateTimeZone.UTC).getMillis()),
                new Double(new DateTime(certificate.getNotAfter().getTime(), DateTimeZone.UTC).getMillis())));
        } catch (CertificateException e) {
            System.err.println(String.format(CERTIFICATE_EXCEPTION_MESSAGE, entityId, certificate.getCertificateType()));
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
                Long.toString(DateTime.now(DateTimeZone.UTC).getMillis()),
                new Double(INVALID)));
        } catch (CertificateException e) {
            System.err.println(String.format(CERTIFICATE_EXCEPTION_MESSAGE, entityId, certificate.getCertificateType()));
        }
        return Optional.empty();
    }
}
