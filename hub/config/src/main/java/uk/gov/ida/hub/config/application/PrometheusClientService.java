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
    private final Gauge gauge;

    @Inject
    public PrometheusClientService(final CertificateService certificateService,
                                   final Gauge gauge) {
        this.certificateService = certificateService;
        this.gauge = gauge;
    }

    public void createCertificateExpiryMetrics() {
        final Set<CertificateDetails> certificateDetailsSet = certificateService.getAllCertificatesDetails();
        certificateDetailsSet.forEach(
            certificateDetails -> {
                try {
                    gauge.labels(
                        certificateDetails.getIssuerId(),
                        certificateDetails.getCertificate().getCertificateType().toString(),
                        certificateDetails.getCertificate().getFingerprint())
                         .set(certificateDetails.getCertificate().getNotAfter().getTime());
                } catch (CertificateException e) {
                    LOG.warn(String.format("Invalid X.509 certificate [issuer id: %s]", certificateDetails.getIssuerId()));
                }
            });
    }
}
