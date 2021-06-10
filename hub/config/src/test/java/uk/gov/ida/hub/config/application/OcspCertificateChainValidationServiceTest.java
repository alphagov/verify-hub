package uk.gov.ida.hub.config.application;

import io.prometheus.client.Gauge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateOrigin;
import uk.gov.ida.hub.config.domain.CertificateUse;
import uk.gov.ida.hub.config.domain.OCSPCertificateChainValidityChecker;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.saml.core.test.TestCertificateStrings;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OcspCertificateChainValidationServiceTest {

    private static final Certificate cert1 = makeCertificate(TestCertificateStrings.TEST_RP_PUBLIC_SIGNING_CERT);
    private static final Certificate cert2 = makeCertificate(TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT);
    private static final Certificate cert3 = makeCertificate(TestCertificateStrings.UNCHAINED_PUBLIC_CERT);

    @Mock
    private OCSPCertificateChainValidityChecker ocspCertificateChainValidityChecker;

    @Mock
    private CertificateService certificateService;

    @Test
    public void gaugesAreUpdatedForCertsWithValidChains() throws Exception {
        Gauge ocspStatusGauge = Gauge.build("ocspStatusGauge", "whatever")
                .labelNames("entity_id", "use", "subject", "fingerprint", "serial")
                .create();
        Gauge lastUpdatedGauge = Gauge.build("lastUpdatedGauge", "whatever")
                .labelNames("entity_id", "use", "subject", "fingerprint", "serial")
                .create();

        OcspCertificateChainValidationService ocspCertificateChainValidationService = new OcspCertificateChainValidationService(
                ocspCertificateChainValidityChecker,
                certificateService,
                ocspStatusGauge,
                lastUpdatedGauge
        );


        // Check gauges are initially set to zero.
        assertThat(getGaugeValue(ocspStatusGauge, cert1)).isEqualTo(0);
        assertThat(getGaugeValue(ocspStatusGauge, cert2)).isEqualTo(0);
        assertThat(getGaugeValue(ocspStatusGauge, cert3)).isEqualTo(0);

        assertThat(getGaugeValue(lastUpdatedGauge, cert1)).isEqualTo(0);
        assertThat(getGaugeValue(lastUpdatedGauge, cert2)).isEqualTo(0);
        assertThat(getGaugeValue(lastUpdatedGauge, cert3)).isEqualTo(0);


        // Run the service and check gauges.

        when(certificateService.getAllCertificates()).thenReturn(Set.of(cert1, cert2, cert3));
        when(ocspCertificateChainValidityChecker.isValid(cert1)).thenReturn(true);
        when(ocspCertificateChainValidityChecker.isValid(cert2)).thenReturn(true);
        when(ocspCertificateChainValidityChecker.isValid(cert3)).thenReturn(false);

        ocspCertificateChainValidationService.run();

        assertThat(getGaugeValue(ocspStatusGauge, cert1)).isEqualTo(1);
        assertThat(getGaugeValue(ocspStatusGauge, cert2)).isEqualTo(1);
        assertThat(getGaugeValue(ocspStatusGauge, cert3)).isEqualTo(0);

        assertThat(getGaugeValue(lastUpdatedGauge, cert1)).isGreaterThan(0);
        assertThat(getGaugeValue(lastUpdatedGauge, cert2)).isGreaterThan(0);
        assertThat(getGaugeValue(lastUpdatedGauge, cert3)).isEqualTo(0);


        // Make cert2 invalid and run the service again.
        when(ocspCertificateChainValidityChecker.isValid(cert2)).thenReturn(false);

        ocspCertificateChainValidationService.run();

        assertThat(getGaugeValue(ocspStatusGauge, cert1)).isEqualTo(1);
        assertThat(getGaugeValue(ocspStatusGauge, cert2)).isEqualTo(0);
        assertThat(getGaugeValue(ocspStatusGauge, cert3)).isEqualTo(0);

        assertThat(getGaugeValue(lastUpdatedGauge, cert1)).isGreaterThan(0);
        assertThat(getGaugeValue(lastUpdatedGauge, cert2)).isGreaterThan(0);
        assertThat(getGaugeValue(lastUpdatedGauge, cert3)).isEqualTo(0);
    }

    private static Certificate makeCertificate(String x509) {
        return new Certificate(
                "issuerEntityId",
                FederationEntityType.RP,
                x509,
                CertificateUse.SIGNING,
                CertificateOrigin.FEDERATION,
                true
        );
    }

    private static double getGaugeValue(Gauge gauge, Certificate certificate) throws Exception {
        return gauge.labels(certificate.getIssuerEntityId(),
                certificate.getCertificateUse().toString(),
                certificate.getSubject(),
                certificate.getFingerprint(),
                String.valueOf(certificate.getSerialNumber())).get();
    }

}
