package uk.gov.ida.hub.config.dto;

import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.List;

public class IdpConfigDto {

    private String simpleId;
    private Boolean enabled;
    private List<LevelOfAssurance> supportedLevelsOfAssurance;
    private Boolean useExactComparisonType;

    @SuppressWarnings("unused") // NEEDED BY JAXB
    protected IdpConfigDto() {
    }

    public IdpConfigDto(
            String simpleId,
            Boolean enabled,
            List<LevelOfAssurance> supportedLevelsOfAssurance,
            Boolean useExactComparisonType) {
        this.simpleId = simpleId;
        this.enabled = enabled;
        this.supportedLevelsOfAssurance = supportedLevelsOfAssurance;
        this.useExactComparisonType = useExactComparisonType;
    }

    public String getSimpleId() {
        return simpleId;
    }

    public Boolean isEnabled() { return enabled; }

    public List<LevelOfAssurance> getSupportedLevelsOfAssurance() {
        return supportedLevelsOfAssurance;
    }

    public Boolean getUseExactComparisonType() {
        return useExactComparisonType;
    }
}
