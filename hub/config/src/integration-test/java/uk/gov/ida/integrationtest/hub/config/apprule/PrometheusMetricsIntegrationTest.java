package uk.gov.ida.integrationtest.hub.config.apprule;

import io.dropwizard.testing.ResourceHelpers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.integrationtest.hub.config.apprule.support.ConfigAppExtension;
import uk.gov.ida.integrationtest.hub.config.apprule.support.Message;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.ws.rs.core.Response;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.application.OcspCertificateChainValidationService.INVALID;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_HELP;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED_HELP;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_OCSP_LAST_SUCCESS_TIMESTAMP;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_OCSP_LAST_SUCCESS_TIMESTAMP_HELP;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS;
import static uk.gov.ida.hub.config.application.PrometheusClientService.VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS_HELP;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;
import static uk.gov.ida.integrationtest.hub.config.apprule.support.Message.messageShouldBePresent;
import static uk.gov.ida.integrationtest.hub.config.apprule.support.Message.messageShouldNotBePresent;

public class PrometheusMetricsIntegrationTest {
    private static ClientSupport client;
    private static final int WAIT_FOR_CERTIFICATE_METRICS_TO_BE_UPDATED = 3_000;
    private static final String GAUGE_HELP_TEMPLATE = "# HELP %s %s\n";
    private static final String GAUGE_TYPE_TEMPLATE = "# TYPE %s gauge\n";
    private static final String CERTIFICATE_EXCEPTION_MESSAGE = "Unable to get NotAfter from the certificate [issuer = %s, type = %s]";
    private static final String RP_ENTITY_ID = "rp-entity-id";
    private static final String RP_MS_ENTITY_ID = "rp-ms-entity-id";
    private static final String CERTIFICATE_METRICS_TEMPLATE = "%s{entity_id=\"%s\",use=\"%s\",subject=\"%s\",fingerprint=\"%s\",serial=\"%s\",} %s\n";
    private static final String LAST_UPDATE_METRICS_TEMPLATE = "%s %s\n";
    private static final TransactionConfig TRANSACTION_CONFIG_ENTITY_DATA = aTransactionConfigData().withEntityId(RP_ENTITY_ID)
                                                                                                              .withMatchingServiceEntityId(RP_MS_ENTITY_ID)
                                                                                                              .build();
    private static final MatchingServiceConfig MATCHING_SERVICE_CONFIG_ENTITY_DATA = aMatchingServiceConfig().withEntityId(RP_MS_ENTITY_ID)
                                                                                                                                 .build();
    private static final IdentityProviderConfig IDENTITY_PROVIDER_CONFIG_ENTITY_DATA = anIdentityProviderConfigData().withEntityId("idp-entity-id")
                                                                                                                               .withOnboarding(singletonList(RP_ENTITY_ID))
                                                                                                                               .build();
    private static final String RP_ENTITY_ID_BAD_SIGNATURE_CERT = "rp-entity-id-bad-cert";
    private static final String RP_ENTITY_ID_BAD_ENCRYPTION_CERT = "rp-entity-id-bad-encryption-cert";
    private static final String BAD_CERTIFICATE_VALUE = "MIIEZzCCA0+gAwIBAgIQX/UeEoUFa9978uQ8FbLFyDANBgkqhkiG9w0BAQsFADBZMQswCQYDVQQGEwJHQjEXMBUGA1UEChMOQ2FiaW5ldCBPZmZpY2UxDDAKBgNVBAsTA0dEUzEjMCEGA1UEAxMaSURBUCBSZWx5aW5nIFBhcnR5IFRlc3QgQ0EwHhcNMTUwODI3MDAwMDAwWhcNMTcwODI2MjM1OTU5WjCBgzELMAkGA1UEBhMCR0IxDzANBgNVBAgTBkxvbmRvbjEPMA0GA1UEBxMGTG9uZG9uMRcwFQYDVQQKFA5DYWJpbmV0IE9mZmljZTEMMAoGA1UECxQDR0RTMSswKQYDVQQDEyJTYW1wbGUgUlAgU2lnbmluZyAoMjAxNTA4MjYxNjMzMDcpMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuIdy6fiwdlLpMOsOiZC8DXcAU1eKDKz0w04TRAdUMR4rdv36IcyTfUortDHQ60pmX4I/s5iksey4UHCqTNZKpw6coCboyFGtGy1M6tTFhrxKc/pZmjEqV0kqgfjUnVWqiOnjpuWOJsCRfScjGfJ4Gio0omnrfX6KOTrnieaSM7aZJ7WkWUe4KRGOyxBywRIyFFbUeNgIbD/IfV7GFZCLUa9XwKjnaidTTmEhihC0TiBcnl3NCeqSwNK0TsIYSh/k5i7U/QeIvc6w34lacHOsqL5woRMPBnmS91brY/hy/vdePx7Nk8Hiwx7VpLsn5b0BVJnEZcLs5gwDid0Vra+6kQIDAQABo4H/MIH8MAwGA1UdEwEB/wQCMAAwYQYDVR0fBFowWDBWoFSgUoZQaHR0cDovL29uc2l0ZWNybC50cnVzdHdpc2UuY29tL0NhYmluZXRPZmZpY2VJREFQUmVseWluZ1BhcnR5VGVzdENBL0xhdGVzdENSTC5jcmwwDgYDVR0PAQH/BAQDAgeAMB0GA1UdDgQWBBSsYjo5j/oZAQ/h35orm1VR+n5hVTAfBgNVHSMEGDAWgBTd5PVdGgoPOtFIIh5OwPhuNvbFJTA5BggrBgEFBQcBAQQtMCswKQYIKwYBBQUHMAGGHWh0dHA6Ly9zdGQtb2NzcC50cnVzdHdpc2UuY29tMA0GCSqGSIb3DQEBCwUAA4IBAQBHYp/kWufCENWW8xI/rwVRJrOjvYxbhyEM61QoMZzTqfSQVuaBCv1qwXTMU8D+iPVtSVStFdU+vxWrU0z8ZQcd9107wZtnIJWwoJJ4WJlrmXTzBNvlqc8Q57G4Y/x9SZZdyVn4JrQRK8Vm5NzZqYZeXqgMk5xeQEObY8EQFmdryZeh/B2j0WFm3ywXOYcz77a1e1WCxBgOULPh1sQD793KjbJlEUfyeq5w/cIPovI8u4xXa78ionzq+L9t3oRh/wuTNjG/qezgArncr53sV2RZzb45RtT9+PxdQ1YFbQM7lL526kxVij0+FS6+b+EBx2CBVLWalmOugi0vA9vYpZJL";
    private static final String BAD_SIGNATURE_CERTIFICATE = BAD_CERTIFICATE_VALUE;
    private static final String BAD_ENCRYPTION_CERTIFICATE = BAD_CERTIFICATE_VALUE;

    @RegisterExtension
    static TestDropwizardAppExtension app = ConfigAppExtension.forApp(ConfigApplication.class)
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
            .addIdp(IDENTITY_PROVIDER_CONFIG_ENTITY_DATA)
            .writeFederationConfig()
            .withClearedCollectorRegistry()
            .withDefaultConfigOverridesAnd(
                    "certificateExpiryDateCheckServiceConfiguration.enable: true",
                    "certificateExpiryDateCheckServiceConfiguration.initialDelay: 1s",
                    "certificateExpiryDateCheckServiceConfiguration.delay: 2s",
                    "certificateOcspRevocationStatusCheckServiceConfiguration.enable: true",
                    "certificateOcspRevocationStatusCheckServiceConfiguration.initialDelay: 1s",
                    "certificateOcspRevocationStatusCheckServiceConfiguration.delay: 2s"
            )
            .config(ResourceHelpers.resourceFilePath("config.yml"))
            .randomPorts()
            .create();

    @BeforeAll
    public static void setUpBeforeClass(ClientSupport clientSupport) {
        client = clientSupport;
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

        assertThat(entity).contains(String.format(GAUGE_HELP_TEMPLATE, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_HELP));
        assertThat(entity).contains(String.format(GAUGE_TYPE_TEMPLATE, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE));
        assertThat(entity).contains(String.format(GAUGE_HELP_TEMPLATE, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED_HELP));
        assertThat(entity).contains(String.format(GAUGE_TYPE_TEMPLATE, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED));

        assertThat(entity).contains(String.format(GAUGE_HELP_TEMPLATE, VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS, VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS_HELP));
        assertThat(entity).contains(String.format(GAUGE_TYPE_TEMPLATE, VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS));
        assertThat(entity).contains(String.format(GAUGE_HELP_TEMPLATE, VERIFY_CONFIG_CERTIFICATE_OCSP_LAST_SUCCESS_TIMESTAMP, VERIFY_CONFIG_CERTIFICATE_OCSP_LAST_SUCCESS_TIMESTAMP_HELP));
        assertThat(entity).contains(String.format(GAUGE_TYPE_TEMPLATE, VERIFY_CONFIG_CERTIFICATE_OCSP_LAST_SUCCESS_TIMESTAMP));

        getExpectedCertificatesMetrics().stream()
                                        .filter(Message::isPresent)
                                        .forEach(expectedMessage -> assertThat(entity).contains(expectedMessage.getMessage()));
        getExpectedCertificatesMetrics().stream()
                                        .filter(expectedMessage -> !expectedMessage.isPresent())
                                        .forEach(expectedMessage -> assertThat(entity).doesNotContain(expectedMessage.getMessage()));
    }

    private Response getPrometheusMetrics() {
        return client.targetAdmin("/prometheus/metrics").request().buildGet().invoke();
    }

    private List<Message> getExpectedCertificatesMetrics() {
        List<Message> expectedCertificatesMetrics = new ArrayList<>();
        expectedCertificatesMetrics.addAll(getExpectedCertificateMetricsList(TRANSACTION_CONFIG_ENTITY_DATA));
        expectedCertificatesMetrics.addAll(getExpectedCertificateMetricsList(MATCHING_SERVICE_CONFIG_ENTITY_DATA));
        return expectedCertificatesMetrics;
    }

    private <T extends CertificateConfigurable<T>> List<Message> getExpectedCertificateMetricsList(final T configEntityData) {
        List<Message> expectedCertificatesMetrics = new ArrayList<>();
        configEntityData.getSignatureVerificationCertificates()
                        .forEach(
                            certificate -> {
                                getExpectedCertificateExpiryDateMetrics(configEntityData.getEntityId(), certificate).ifPresent(
                                        expectedCertificatesMetrics::add);
                                getExpectedCertificateOcspRevocationStatusMetrics(configEntityData.getEntityId(), certificate).ifPresent(
                                        expectedCertificatesMetrics::addAll);
                            });
        getExpectedCertificateExpiryDateMetrics(configEntityData.getEntityId(), configEntityData.getEncryptionCertificate()).ifPresent(
                expectedCertificatesMetrics::add);
        getExpectedCertificateOcspRevocationStatusMetrics(configEntityData.getEntityId(), configEntityData.getEncryptionCertificate()).ifPresent(
                expectedCertificatesMetrics::addAll);
        addExpectedCertificateExpiryDateLastUpdate(expectedCertificatesMetrics);
        return expectedCertificatesMetrics;
    }

    private void addExpectedCertificateExpiryDateLastUpdate(final List<Message> expectedCertificatesMetrics) {
        expectedCertificatesMetrics.add(messageShouldBePresent(String.format(LAST_UPDATE_METRICS_TEMPLATE,
            VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED,
            (double) DateTime.now(DateTimeZone.UTC).getMillis())));
    }

    private Optional<Message> getExpectedCertificateExpiryDateMetrics(final String entityId,
                                                                      final Certificate certificate) {
        try {
            return Optional.of(
                messageShouldBePresent(
                    getExpectedCertificateMetric(VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE,
                        entityId,
                        certificate,
                        new DateTime(certificate.getNotAfter().getTime(), DateTimeZone.UTC).getMillis())));
        } catch (CertificateException e) {
            System.err.println(String.format(CERTIFICATE_EXCEPTION_MESSAGE, entityId, certificate.getCertificateUse()));
        }
        return Optional.empty();
    }

    private Optional<List<Message>> getExpectedCertificateOcspRevocationStatusMetrics(final String entityId,
                                                                                      final Certificate certificate) {
        try {
            return Optional.of(
                Arrays.asList(
                    messageShouldBePresent(getExpectedCertificateMetric(VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS,
                        entityId,
                        certificate,
                        INVALID)),
                    messageShouldNotBePresent(getExpectedCertificateMetric(VERIFY_CONFIG_CERTIFICATE_OCSP_LAST_SUCCESS_TIMESTAMP,
                        entityId,
                        certificate,
                        DateTime.now(DateTimeZone.UTC).getMillis()))));
        } catch (CertificateException e) {
            System.err.println(String.format(CERTIFICATE_EXCEPTION_MESSAGE, entityId, certificate.getCertificateUse()));
        }
        return Optional.empty();
    }

    private String getExpectedCertificateMetric(final String metricName,
                                                final String entityId,
                                                final Certificate certificate,
                                                final double value) throws CertificateException {
        return String.format(CERTIFICATE_METRICS_TEMPLATE,
            metricName,
            entityId,
            certificate.getCertificateUse(),
            certificate.getSubject(),
            certificate.getFingerprint(),
            certificate.getSerialNumber(),
            value);
    }
}
