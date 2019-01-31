package uk.gov.ida.hub.config.application;

import io.dropwizard.setup.Environment;
import io.prometheus.client.Gauge;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.configuration.PrometheusClientServiceConfiguration;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PrometheusClientService {
    public static final String VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE = "verify_config_certificate_expiry_date";
    public static final String VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED = "verify_config_certificate_expiry_date_last_updated";
    public static final String VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS = "verify_config_certificate_ocsp_revocation_status";
    public static final String VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS_LAST_UPDATED = "verify_config_certificate_ocsp_revocation_status_last_updated";
    public static final String VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_HELP = "X.509 Certificate Expiry Date (ms)";
    public static final String VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED_HELP = "X.509 Certificate Expiry Date Metric Last Updated (ms)";
    public static final String VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS_HELP = "X.509 Certificate OCSP Revocation Status (1 = valid and 0 = invalid)";
    public static final String VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS_LAST_UPDATED_HELP = "X.509 Certificate OCSP Revocation Status Metric Last Updated (ms)";
    private static final boolean USE_DAEMON_THREADS = true;
    private final Environment environment;
    private final ConfigConfiguration configConfiguration;
    private final CertificateService certificateService;
    private final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker;

    @Inject
    public PrometheusClientService(final Environment environment,
                                   final ConfigConfiguration configConfiguration,
                                   final CertificateService certificateService,
                                   final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker) {
        this.environment = environment;
        this.configConfiguration = configConfiguration;
        this.certificateService = certificateService;
        this.ocspCertificateChainValidityChecker = ocspCertificateChainValidityChecker;
    }

    public void createCertificateExpiryDateCheckMetrics() {
        final PrometheusClientServiceConfiguration configuration = configConfiguration.getCertificateExpiryDateCheckServiceConfiguration();
        if (configuration.getEnable()) {
            Gauge expiryDateGauge = Gauge.build(VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_HELP)
                                         .labelNames("entity_id", "use", "subject", "fingerprint", "serial")
                                         .register();
            Gauge lastUpdatedGauge = Gauge.build(VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE_LAST_UPDATED_HELP)
                                          .register();

            CertificateExpiryDateCheckService certificateExpiryDateCheckService = new CertificateExpiryDateCheckService(
                certificateService,
                expiryDateGauge,
                lastUpdatedGauge);

            createScheduledExecutorService(configuration, VERIFY_CONFIG_CERTIFICATE_EXPIRY_DATE, certificateExpiryDateCheckService);
        }
    }

    public void createCertificateOcspRevocationStatusCheckMetrics() {
        final PrometheusClientServiceConfiguration configuration = configConfiguration.getCertificateOcspRevocationStatusCheckServiceConfiguration();
        if (configuration.getEnable()) {
            Gauge ocspStatusGauge = Gauge.build(VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS, VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS_HELP)
                                         .labelNames("entity_id", "use", "subject", "fingerprint", "serial")
                                         .register();
            Gauge lastUpdatedGauge = Gauge.build(VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS_LAST_UPDATED, VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS_LAST_UPDATED_HELP)
                                          .labelNames("entity_id", "use", "subject", "fingerprint", "serial")
                                          .register();

            OcspCertificateChainValidationService ocspCertificateChainValidationService = new OcspCertificateChainValidationService(
                ocspCertificateChainValidityChecker,
                certificateService,
                ocspStatusGauge,
                lastUpdatedGauge);

            createScheduledExecutorService(configuration, VERIFY_CONFIG_CERTIFICATE_OCSP_REVOCATION_STATUS, ocspCertificateChainValidationService);
        }
    }

    private void createScheduledExecutorService(PrometheusClientServiceConfiguration configuration,
                                                String nameFormat,
                                                Runnable service) {
        ScheduledExecutorService scheduledExecutorService = environment.lifecycle()
                                                                       .scheduledExecutorService(nameFormat, USE_DAEMON_THREADS)
                                                                       .build();
        scheduledExecutorService.scheduleWithFixedDelay(
            service,
            configuration.getInitialDelay().toSeconds(),
            configuration.getDelay().toSeconds(),
            TimeUnit.SECONDS);
    }
}
