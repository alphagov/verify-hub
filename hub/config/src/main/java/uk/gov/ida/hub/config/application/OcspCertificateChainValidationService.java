package uk.gov.ida.hub.config.application;

import io.prometheus.client.Gauge;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;

import java.security.cert.CertificateException;
import java.util.Set;

public class OcspCertificateChainValidationService implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(OcspCertificateChainValidationService.class);
    public static final double VALID = 1.0;
    public static final double INVALID = 0.0;
    private final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker;
    private final CertificateService certificateService;
    private final Gauge ocspStatusGauge;
    private final Gauge lastUpdatedGauge;

    public OcspCertificateChainValidationService(final OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker,
                                                 final CertificateService certificateService,
                                                 final Gauge ocspStatusGauge,
                                                 final Gauge lastUpdatedGauge) {
        this.ocspCertificateChainValidityChecker = ocspCertificateChainValidityChecker;
        this.certificateService = certificateService;
        this.ocspStatusGauge = ocspStatusGauge;
        this.lastUpdatedGauge = lastUpdatedGauge;
    }

    @Override
    public void run() {
        try {
            final Set<CertificateDetails> certificateDetailsSet = certificateService.getAllCertificatesDetails();
            final double timestamp = DateTime.now(DateTimeZone.UTC).getMillis();
            certificateDetailsSet.forEach(certificateDetails -> {
                try {
                    addAGauge(
                        ocspStatusGauge,
                        certificateDetails,
                        ocspCertificateChainValidityChecker.isValid(
                            certificateDetails.getCertificate(),
                            certificateDetails.getFederationEntityType()) ? VALID : INVALID);
                    addAGauge(lastUpdatedGauge, certificateDetails, timestamp);
                } catch (CertificateException e) {
                    LOG.warn(String.format("Invalid X.509 certificate [issuer id: %s]", certificateDetails.getIssuerId()));
                }
            });
            LOG.info("Updated Certificates OCSP Revocation Statuses Metrics.");
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    private void addAGauge(final Gauge gauge,
                           final CertificateDetails certificateDetails,
                           final double value) throws CertificateException {
        gauge.labels(certificateDetails.getIssuerId(),
            certificateDetails.getCertificate().getCertificateType().toString(),
            certificateDetails.getCertificate().getSubject(),
            certificateDetails.getCertificate().getFingerprint(),
            String.valueOf(certificateDetails.getCertificate().getSerialNumber()))
             .set(value);
    }
}
