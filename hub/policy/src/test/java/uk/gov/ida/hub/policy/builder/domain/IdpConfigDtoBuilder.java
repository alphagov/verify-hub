package uk.gov.ida.hub.policy.builder.domain;

import com.google.common.collect.ImmutableList;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;

public class IdpConfigDtoBuilder {

    private String simpleId = "an-idp";
    private Boolean enabled = true;
    private List<LevelOfAssurance> supportedLevelsOfAssurance = of(LevelOfAssurance.LEVEL_1);

    public static IdpConfigDtoBuilder anIdpConfigDto() {
        return new IdpConfigDtoBuilder();
    }

    public IdpConfigDtoBuilder withLevelsOfAssurance(List<LevelOfAssurance> levelOfAssurances) {
        this.supportedLevelsOfAssurance = levelOfAssurances;
        return this;
    }

    public IdpConfigDtoBuilder withLevelsOfAssurance(LevelOfAssurance... levelsOfAssurance) {
        this.supportedLevelsOfAssurance = ImmutableList.copyOf(levelsOfAssurance);
        return this;
    }

    public IdpConfigDto build() {
        return new IdpConfigDto(simpleId, enabled, supportedLevelsOfAssurance);
    }

}
