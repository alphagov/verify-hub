package uk.gov.ida.hub.config.application;

import io.dropwizard.setup.Environment;
import io.prometheus.client.Gauge;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.configuration.PrometheusClientServiceConfiguration;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PrometheusClientService {
    public static final String VERIFY_CONFIG_CERTIFICATE_EXPIRY = "verify_config_certificate_expiry";
    private static final boolean USE_DAEMON_THREADS = true;
    private final Environment environment;
    private final ConfigConfiguration configConfiguration;
    private final CertificateService certificateService;

    @Inject
    public PrometheusClientService(final Environment environment,
                                   final ConfigConfiguration configConfiguration,
                                   final CertificateService certificateService) {
        this.environment = environment;
        this.configConfiguration = configConfiguration;
        this.certificateService = certificateService;
    }

    public void createCertificateExpiryDateCheckMetrics() {
        final PrometheusClientServiceConfiguration configuration = configConfiguration.getCertificateExpiryDateCheckServiceConfiguration();
        if (configuration.getEnable()) {
            Gauge gauge = Gauge.build(VERIFY_CONFIG_CERTIFICATE_EXPIRY, "Timestamp of NotAfter value of X.509 certificate")
                               .labelNames("entity_id", "use", "subject", "fingerprint", "timestamp")
                               .register();

            CertificateExpiryDateCheckService certificateExpiryDateCheckService = new CertificateExpiryDateCheckService(certificateService, gauge);

            ScheduledExecutorService scheduledCertificateExpiryDateCheckService =
                environment.lifecycle()
                           .scheduledExecutorService(VERIFY_CONFIG_CERTIFICATE_EXPIRY, USE_DAEMON_THREADS)
                           .build();
            scheduledCertificateExpiryDateCheckService.scheduleWithFixedDelay(
                certificateExpiryDateCheckService,
                configuration.getInitialDelay().toSeconds(),
                configuration.getDelay().toSeconds(),
                TimeUnit.SECONDS);
        }
    }
}
