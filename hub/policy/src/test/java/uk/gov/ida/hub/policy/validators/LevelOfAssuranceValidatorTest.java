package uk.gov.ida.hub.policy.validators;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class LevelOfAssuranceValidatorTest {

    private LevelOfAssuranceValidator levelOfAssuranceValidator;

    @Before
    public void setup() {
        levelOfAssuranceValidator = new LevelOfAssuranceValidator();
    }

    @Test
    public void validate_shouldNotThrowExceptionIfLevelOfAssuranceFromMatchingServiceMatchesOneFromIdp() throws Exception {
        LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
        levelOfAssuranceValidator.validate(Optional.fromNullable(levelOfAssurance), levelOfAssurance);
    }

    @Test
    public void validate_shouldThrowExceptionIfLevelOfAssuranceFromMatchingServiceDoesNotExist() throws Exception {
        LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
        try {
            levelOfAssuranceValidator.validate(Optional.<LevelOfAssurance>absent(), levelOfAssurance);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo(StateProcessingValidationException.noLevelOfAssurance().getMessage());
        }
    }

    @Test
    public void validate_shouldThrowExceptionIfLevelOfAssuranceFromMatchingServiceDoesNotMatchOneFromIdp() throws Exception {
        try {
            levelOfAssuranceValidator.validate(Optional.fromNullable(LevelOfAssurance.LEVEL_2), LevelOfAssurance.LEVEL_4);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo(StateProcessingValidationException.wrongLevelOfAssurance(java.util.Optional.of(LevelOfAssurance.LEVEL_2), singletonList(LevelOfAssurance.LEVEL_4)).getMessage());
        }
    }
}
