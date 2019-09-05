package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;

import java.util.List;

public class IdpConfigDtoBuilder {

    private String simpleId = "an-idp";
    private Boolean enabled = true;
    private List<LevelOfAssurance> supportedLevelsOfAssurance = List.of(LevelOfAssurance.LEVEL_1);

    public static IdpConfigDtoBuilder anIdpConfigDto() {
        return new IdpConfigDtoBuilder();
    }

    public IdpConfigDtoBuilder withLevelsOfAssurance(List<LevelOfAssurance> levelOfAssurances) {
        this.supportedLevelsOfAssurance = levelOfAssurances;
        return this;
    }

    public IdpConfigDtoBuilder withLevelsOfAssurance(LevelOfAssurance... levelsOfAssurance) {
        this.supportedLevelsOfAssurance = List.of(levelsOfAssurance);
        return this;
    }

    public IdpConfigDto build() {
        return new IdpConfigDto(simpleId, enabled, supportedLevelsOfAssurance);
    }

}
