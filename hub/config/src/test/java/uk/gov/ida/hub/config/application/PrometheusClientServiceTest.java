package uk.gov.ida.hub.config.application;


import io.prometheus.client.Collector;
import io.prometheus.client.Gauge;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder;
import uk.gov.ida.hub.config.dto.FederationEntityType;

import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PrometheusClientServiceTest {
    private static final String VERIFY_CONFIG_CERTIFICATE_EXPIRY = "verify_config_certificate_expiry";
    private static final String ENTITY_ID = "entity_id";
    private static final String USE = "use";
    private static final String FINGERPRINT = "fingerprint";
    private static final DateTime NOW = DateTime.now(DateTimeZone.UTC);
    private static final String RP_ENTITY_ID = "rpEntityId";
    private static final SignatureVerificationCertificate CERTIFICATE = new SignatureVerificationCertificateBuilder().build();
    private PrometheusClientService prometheusClientService;
    private Gauge gauge;
    private Set<CertificateDetails> certificateDetailsSet = new HashSet<>();
    private String labelNames[] = {ENTITY_ID, USE, FINGERPRINT};

    @Mock
    private CertificateService certificateService;

    @Before
    public void setUp() {
        final CertificateDetails certificateDetails = new CertificateDetails(RP_ENTITY_ID, CERTIFICATE, FederationEntityType.RP, true);
        certificateDetailsSet.add(certificateDetails);
        gauge = Gauge.build(VERIFY_CONFIG_CERTIFICATE_EXPIRY, "Timestamp of NotAfter value of X.509 certificate")
                     .labelNames(labelNames)
                     .register();
        prometheusClientService = new PrometheusClientService(certificateService, gauge);
    }

    @Test
    public void shouldCreateCertificatesMetrics() throws CertificateException {
        List<String> expectedValues = new ArrayList<>();
        expectedValues.add(RP_ENTITY_ID);
        expectedValues.add(CERTIFICATE.getCertificateType().toString());
        expectedValues.add(CERTIFICATE.getFingerprint());

        when(certificateService.getAllCertificatesDetails()).thenReturn(certificateDetailsSet);

        prometheusClientService.createCertificateExpiryMetrics();

        verify(certificateService).getAllCertificatesDetails();
        final List<Collector.MetricFamilySamples.Sample> samples = gauge.collect().get(0).samples;
        final Collector.MetricFamilySamples.Sample sample = samples.get(0);
        assertThat(sample.name).isEqualTo(VERIFY_CONFIG_CERTIFICATE_EXPIRY);
        assertThat(sample.labelNames).containsExactly(labelNames);
        assertThat(sample.value).isEqualTo(NOW.getMillis());
        assertThat(sample.labelValues).containsAll(expectedValues);
    }
}
