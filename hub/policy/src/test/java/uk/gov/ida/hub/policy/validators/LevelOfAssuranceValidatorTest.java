package uk.gov.ida.hub.policy.validators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;

import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class LevelOfAssuranceValidatorTest {

    private static LevelOfAssuranceValidator levelOfAssuranceValidator;

    @BeforeAll
    public static void setup() {
        levelOfAssuranceValidator = new LevelOfAssuranceValidator();
    }

    @Test
    public void validate_shouldNotThrowExceptionIfLevelOfAssuranceFromMatchingServiceMatchesOneFromIdp() throws Exception {
        LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
        levelOfAssuranceValidator.validate(Optional.ofNullable(levelOfAssurance), levelOfAssurance);
    }

    @Test
    public void validate_shouldThrowExceptionIfLevelOfAssuranceFromMatchingServiceDoesNotExist() throws Exception {
        LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
        try {
            levelOfAssuranceValidator.validate(Optional.empty(), levelOfAssurance);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo(StateProcessingValidationException.noLevelOfAssurance().getMessage());
        }
    }

    @Test
    public void validate_shouldThrowExceptionIfLevelOfAssuranceFromMatchingServiceDoesNotMatchOneFromIdp() throws Exception {
        try {
            levelOfAssuranceValidator.validate(Optional.ofNullable(LevelOfAssurance.LEVEL_2), LevelOfAssurance.LEVEL_4);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo(StateProcessingValidationException.wrongLevelOfAssurance(java.util.Optional.of(LevelOfAssurance.LEVEL_2), singletonList(LevelOfAssurance.LEVEL_4)).getMessage());
        }
    }
}
