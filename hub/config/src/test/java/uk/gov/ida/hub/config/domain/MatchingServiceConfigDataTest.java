package uk.gov.ida.hub.config.domain;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.shared.ValidationTestHelper.runValidations;

public class MatchingServiceConfigDataTest {

    @Test
    public void isOrganisationUrlValid_shouldReturnViolationIfUrlIsRelative() throws Exception {
        MatchingServiceConfigEntityData data = aMatchingServiceConfigEntityData().withUri(URI.create("/relative")).build();

        Set<ConstraintViolation<MatchingServiceConfigEntityData>> constraintViolations = runValidations(data);

        assertThat(constraintViolations.size()).isEqualTo(1);
        assertThat(constraintViolations.iterator().next().getMessage()).isEqualTo("Matching Service url must be an absolute url.");
    }

    @Test
    public void isOrganisationUrlValid_shouldReturnNoViolationsIfUrlIsAbsolute() throws Exception {

        MatchingServiceConfigEntityData data = aMatchingServiceConfigEntityData().withUri(URI.create("http://absolute.com")).build();

        Set<ConstraintViolation<MatchingServiceConfigEntityData>> constraintViolations = runValidations(data);

        assertThat(constraintViolations.size()).isEqualTo(0);
    }
}
