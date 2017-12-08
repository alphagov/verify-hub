package uk.gov.ida.hub.policy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpConfigDto {

    private String simpleId;
    private Boolean enabled;
    private List<LevelOfAssurance> supportedLevelsOfAssurance;
    private Boolean useExactComparisonType = false;

    @SuppressWarnings("unused") // NEEDED BY JAXB
    protected IdpConfigDto() {
    }

    public IdpConfigDto(
            String simpleId,
            Boolean enabled,
            List<LevelOfAssurance> supportedLevelsOfAssurance) {
        this.simpleId = simpleId;
        this.enabled = enabled;
        this.supportedLevelsOfAssurance = supportedLevelsOfAssurance;
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
