package uk.gov.ida.hub.config.domain;

import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.AssertionConsumerServiceBuilder.anAssertionConsumerService;
import static uk.gov.ida.hub.shared.ValidationTestHelper.runValidations;

public class AssertionConsumerServiceTest {

    @Test
    public void isUriValid_shouldReturnViolationIfUrlIsRelative() throws Exception {
        AssertionConsumerService data = anAssertionConsumerService().withUri(URI.create("/relative")).build();

        Set<ConstraintViolation<AssertionConsumerService>> constraintViolations = runValidations(data);

        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.iterator().next().getMessage()).isEqualTo("Assertion Consumer Service url must be an absolute url.");
    }

    @Test
    public void isUriValid_shouldReturnNoViolationsIfUrlIsAbsolute() throws Exception {
        AssertionConsumerService data = anAssertionConsumerService().withUri(URI.create("http://absolute.com")).build();

        Set<ConstraintViolation<AssertionConsumerService>> constraintViolations = runValidations(data);

        assertThat(constraintViolations.size()).isEqualTo(0);
    }

    @Test
    public void isIndexValid_shouldReturnNoViolationsIfIndexIsAnUnsignedNumber() throws Exception {
        AssertionConsumerService data = anAssertionConsumerService().withIndex(1).build();

        Set<ConstraintViolation<AssertionConsumerService>> constraintViolations = runValidations(data);

        assertThat(constraintViolations.size()).isEqualTo(0);
    }

    @Test
    public void isIndexValid_shouldReturnNoViolationsIfIndexIsAbsent() throws Exception {
        AssertionConsumerService data = anAssertionConsumerService().build();

        Set<ConstraintViolation<AssertionConsumerService>> constraintViolations = runValidations(data);

        assertThat(constraintViolations.size()).isEqualTo(0);
    }

    @Test
    public void isIndexValid_shouldReturnViolationIfIndexIsASignedNumber() throws Exception {
        AssertionConsumerService data = anAssertionConsumerService().withIndex(-1).build();

        Set<ConstraintViolation<AssertionConsumerService>> constraintViolations = runValidations(data);

        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.iterator().next().getMessage()).isEqualTo("Index must be an unsigned integer.");
    }
}
