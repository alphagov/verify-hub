package uk.gov.ida.hub.config.application;

import io.prometheus.client.Gauge;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;

import java.security.cert.CertificateException;
import java.util.Objects;
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
            final Set<Certificate> certificatesSet = certificateService.getAllCertificates();
            final double timestamp = DateTime.now(DateTimeZone.UTC).getMillis();
            for (Certificate certificate: certificatesSet) {
                try {
                    if (ocspCertificateChainValidityChecker.isValid(certificate)) {
                        updateAGauge(ocspStatusGauge, certificate, VALID);
                        updateAGauge(lastUpdatedGauge, certificate, timestamp);
                    } else {
                        updateAGauge(ocspStatusGauge, certificate, INVALID);
                    }
                } catch (Exception e) {
                    if (Objects.nonNull(certificate)) {
                        // TODO: change this back to error; once we figure how to deal with this in https://govukverify.atlassian.net/browse/HUB-457.
                        LOG.warn(String.format("Unable to set certificates OCSP revocation status metrics for the certificate [issuer id: %s]", certificate.getIssuerEntityId()), e);
                    } else {
                        LOG.error("Unable to set certificates OCSP revocation status metrics.", e);
                    }
                }
            }
            LOG.info("Updated Certificates OCSP Revocation Statuses Metrics.");
        } catch (Exception e) {
            LOG.error("Failed to update Certificates OCSP Revocation Statuses Metrics", e);
        }
    }

    private void updateAGauge(final Gauge gauge,
                              final Certificate certificate,
                              final double value) throws CertificateException {
        gauge.labels(certificate.getIssuerEntityId(),
            certificate.getCertificateUse().toString(),
            certificate.getSubject(),
            certificate.getFingerprint(),
            String.valueOf(certificate.getSerialNumber()))
             .set(value);
    }
}
