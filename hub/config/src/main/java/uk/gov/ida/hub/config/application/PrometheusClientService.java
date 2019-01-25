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
    public static final String VERIFY_CONFIG_CERTIFICATE_EXPIRY = "verify_config_certificate_expiry";
    public static final String VERIFY_CONFIG_CERTIFICATE_OCSP_SUCCESS = "verify_config_certificate_ocsp_success";
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
            Gauge gauge = Gauge.build(VERIFY_CONFIG_CERTIFICATE_EXPIRY, "Timestamp of NotAfter value of X.509 certificate")
                               .labelNames("entity_id", "use", "subject", "fingerprint", "timestamp")
                               .register();

            CertificateExpiryDateCheckService certificateExpiryDateCheckService = new CertificateExpiryDateCheckService(certificateService, gauge);

            createScheduledExecutorService(configuration, VERIFY_CONFIG_CERTIFICATE_EXPIRY, certificateExpiryDateCheckService);
        }
    }

    public void createCertificateOcspRevocationStatusCheckMetrics() {
        final PrometheusClientServiceConfiguration configuration = configConfiguration.getCertificateOcspRevocationStatusCheckServiceConfiguration();
        if (configuration.getEnable()) {
            Gauge gauge = Gauge.build(VERIFY_CONFIG_CERTIFICATE_OCSP_SUCCESS, "Valid X.509 certificate")
                               .labelNames("entity_id", "use", "subject", "fingerprint", "timestamp")
                               .register();

            OcspCertificateChainValidationService ocspCertificateChainValidationService = new OcspCertificateChainValidationService(
                ocspCertificateChainValidityChecker,
                certificateService,
                gauge);

            createScheduledExecutorService(configuration, VERIFY_CONFIG_CERTIFICATE_OCSP_SUCCESS, ocspCertificateChainValidationService);
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
