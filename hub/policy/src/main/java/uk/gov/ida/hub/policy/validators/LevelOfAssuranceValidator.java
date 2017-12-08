package uk.gov.ida.hub.policy.validators;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;

import java.util.Arrays;

import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.noLevelOfAssurance;
import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.wrongLevelOfAssurance;

public class LevelOfAssuranceValidator {

    public void validate(Optional<LevelOfAssurance> responseLevelOfAssurance, LevelOfAssurance requiredLevelOfAssurance) {

        if (!responseLevelOfAssurance.isPresent()) {
            throw noLevelOfAssurance();
        }

        if (!responseLevelOfAssurance.get().equals(requiredLevelOfAssurance)) {
            throw wrongLevelOfAssurance(java.util.Optional.of(responseLevelOfAssurance.get()), Arrays.asList(requiredLevelOfAssurance));
        }
    }

}
