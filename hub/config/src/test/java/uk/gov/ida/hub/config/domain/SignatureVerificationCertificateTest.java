package uk.gov.ida.hub.config.domain;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder;

import javax.validation.ConstraintViolation;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.shared.ValidationTestHelper.runValidations;

public class SignatureVerificationCertificateTest {

    @Test
    public void isX509Valid_shouldReturnViolationIfValueIsInvalid() throws Exception {
        final Certificate certificate = new SignatureVerificationCertificateBuilder().withX509("blah").build();

        final Set<ConstraintViolation<Certificate>> constraintViolations = runValidations(certificate);

        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.iterator().next().getMessage()).isEqualTo("Certificate was not a valid x509 cert.");
    }

    @Test
    public void isX509Valid_shouldReturnNoViolationIfValueIsValid() throws Exception {
        final Certificate certificate = new SignatureVerificationCertificateBuilder().build();

        final Set<ConstraintViolation<Certificate>> constraintViolations = runValidations(certificate);

        assertThat(constraintViolations.size()).isEqualTo(0);
    }

}
