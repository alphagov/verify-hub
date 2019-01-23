package uk.gov.ida.hub.config.application;

import io.prometheus.client.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.CertificateDetails;

import javax.inject.Inject;
import java.security.cert.CertificateException;
import java.util.Set;

public class PrometheusClientService {
    private static final Logger LOG = LoggerFactory.getLogger(PrometheusClientService.class);
    private final CertificateService certificateService;

    @Inject
    public PrometheusClientService(final CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    public void createCertificateExpiryMetrics() {
        final Set<CertificateDetails> certificateDetailsSet = certificateService.getAllCertificatesDetails();
        Gauge gauge = Gauge.build("verify_config_certificate_expiry", "Timestamp of NotAfter value of X.509 certificate")
                           .labelNames("entity_id", "use", "subject", "fingerprint")
                           .register();

        certificateDetailsSet.forEach(
            certificateDetails -> {
                try {
                    gauge.labels(
                        certificateDetails.getIssuerId(),
                        certificateDetails.getCertificate().getCertificateType().toString(),
                        certificateDetails.getCertificate().getSubject(),
                        certificateDetails.getCertificate().getFingerprint())
                         .set(certificateDetails.getCertificate().getNotAfter().getTime());
                } catch (CertificateException e) {
                    LOG.warn(String.format("Invalid X.509 certificate [issuer id: %s]", certificateDetails.getIssuerId()));
                }
            });
    }
}
