package uk.gov.ida.hub.policy.validators;

import uk.gov.ida.hub.policy.domain.LevelOfAssurance;

import java.util.Optional;

import static java.util.Collections.singletonList;
import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.noLevelOfAssurance;
import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.wrongLevelOfAssurance;

public class LevelOfAssuranceValidator {

    public void validate(Optional<LevelOfAssurance> responseLevelOfAssurance, LevelOfAssurance requiredLevelOfAssurance) {

        if (!responseLevelOfAssurance.isPresent()) {
            throw noLevelOfAssurance();
        }

        if (!responseLevelOfAssurance.get().equals(requiredLevelOfAssurance)) {
            throw wrongLevelOfAssurance(java.util.Optional.of(responseLevelOfAssurance.get()), singletonList(requiredLevelOfAssurance));
        }
    }

}
