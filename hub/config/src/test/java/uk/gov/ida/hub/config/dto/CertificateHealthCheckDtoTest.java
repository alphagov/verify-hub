package uk.gov.ida.hub.config.dto;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.builders.EncryptionCertificateBuilder;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import static org.assertj.core.api.Assertions.assertThat;

public class CertificateHealthCheckDtoTest {

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void testCreateCertificateHealthCheckDto() throws Exception {
        final Certificate certificate = new EncryptionCertificateBuilder().build();
        DateTimeFreezer.freezeTime(new DateTime(certificate.getNotAfter()).plusYears(1));

        CertificateHealthCheckDto checked = CertificateHealthCheckDto.createCertificateHealthCheckDto("entityId", certificate, org.joda.time.Duration.millis(1000));
        assertThat(checked.getEntityId()).isEqualTo("entityId");
        assertThat(checked.getStatus()).isEqualTo(CertificateExpiryStatus.CRITICAL);
        assertThat(checked.getMessage()).isEqualTo("EXPIRED");
    }

    @Test
    public void testCreateCertificateHealthCheckDto_forwarning() throws Exception {
        final Certificate certificate = new EncryptionCertificateBuilder().build();
        final DateTime certificateExpiryDate = new DateTime(certificate.getNotAfter());
        DateTimeFreezer.freezeTime(certificateExpiryDate.minusWeeks(1));

        CertificateHealthCheckDto checked = CertificateHealthCheckDto.createCertificateHealthCheckDto("entityId", certificate, org.joda.time.Duration.standardDays(30));
        assertThat(checked.getEntityId()).isEqualTo("entityId");
        assertThat(checked.getStatus()).isEqualTo(CertificateExpiryStatus.WARNING);
        assertThat(checked.getMessage()).isEqualTo("Expires on " + DateTimeFormat.forPattern("EE dd MMM yyyy").print(certificateExpiryDate));
    }

    @Test
    public void testCreateCertificateHealthCheckDto_returnsOK() throws Exception {
        final Certificate certificate = new EncryptionCertificateBuilder().build();
        DateTimeFreezer.freezeTime(new DateTime(certificate.getNotAfter()).minusMonths(3));

        CertificateHealthCheckDto checked = CertificateHealthCheckDto.createCertificateHealthCheckDto("entityId", certificate, org.joda.time.Duration.standardDays(30));
        assertThat(checked.getEntityId()).isEqualTo("entityId");
        assertThat(checked.getStatus()).isEqualTo(CertificateExpiryStatus.OK);
        assertThat(checked.getMessage()).isEmpty();
    }
}
