package uk.gov.ida.hub.config.application;

import io.prometheus.client.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;

import javax.inject.Inject;
import java.security.cert.CertificateException;
import java.util.Set;
import java.util.Timer;

public class PrometheusClientService {
    private static final Logger LOG = LoggerFactory.getLogger(PrometheusClientService.class);
    private final CertificateService certificateService;
    private final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker;

    @Inject
    public PrometheusClientService(final CertificateService certificateService,
                                   final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker) {
        this.certificateService = certificateService;
        this.ocspCertificateChainValidityChecker = ocspCertificateChainValidityChecker;
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

    public void createCertificateOcspSuccessMetrics() {
        Gauge gauge = Gauge.build("verify_config_certificate_ocsp_success", "Valid X.509 certificate")
                           .labelNames("entity_id", "use", "subject", "fingerprint")
                           .register();

        OcspCertificateChainValidationService ocspCertificateChainValidationService =
            new OcspCertificateChainValidationService(
                ocspCertificateChainValidityChecker,
                certificateService,
                gauge);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(ocspCertificateChainValidationService, 0, 60*60*1000);
        LOG.info("OCSP Certificate Chain Validation Service has started.");
    }
}
