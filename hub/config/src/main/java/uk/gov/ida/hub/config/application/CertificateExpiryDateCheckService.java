package uk.gov.ida.hub.config.application;

import io.prometheus.client.Gauge;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.Certificate;

import java.security.cert.CertificateException;
import java.util.Set;

public class CertificateExpiryDateCheckService implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(CertificateExpiryDateCheckService.class);
    private final CertificateService certificateService;
    private final Gauge expiryDateGauge;
    private final Gauge lastUpdatedGauge;

    public CertificateExpiryDateCheckService(final CertificateService certificateService,
                                             final Gauge expiryDateGauge,
                                             final Gauge lastUpdatedGauge) {
        this.certificateService = certificateService;
        this.expiryDateGauge = expiryDateGauge;
        this.lastUpdatedGauge = lastUpdatedGauge;
    }

    @Override
    public void run() {
        try {
            final Set<Certificate> certificateSet = certificateService.getAllCertificates();
            final double timestamp = DateTime.now(DateTimeZone.UTC).getMillis();
            certificateSet.forEach(certificate -> {
                try {
                    expiryDateGauge.labels(certificate.getIssuerEntityId(),
                        certificate.getCertificateType().toString(),
                        certificate.getSubject(),
                        certificate.getFingerprint(),
                        String.valueOf(certificate.getSerialNumber()))
                         .set(certificate.getNotAfter().getTime());
                } catch (CertificateException e) {
                    LOG.warn(String.format("Invalid X.509 certificate [issuer id: %s]", certificate.getIssuerEntityId()));
                }
            });
            lastUpdatedGauge.set(timestamp);
            LOG.info("Updated Certificates Expiry Dates Metrics.");
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }
}
