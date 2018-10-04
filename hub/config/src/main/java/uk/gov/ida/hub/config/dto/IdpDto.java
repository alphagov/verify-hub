package uk.gov.ida.hub.config.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.List;

public class IdpDto {

    private final String simpleId;
    private final String entityId;
    private final List<LevelOfAssurance> levelsOfAssurance;
    private final boolean authenticationEnabled;

    @JsonCreator
    public IdpDto(
            @JsonProperty("simpleId") String simpleId,
            @JsonProperty("entityId") String entityId,
            @JsonProperty("supportedLevelsOfAssurance") List<LevelOfAssurance> levelsOfAssurance,
            @JsonProperty("authenticationEnabled") boolean authenticationEnabled) {
        this.simpleId = simpleId;
        this.entityId = entityId;
        this.levelsOfAssurance = levelsOfAssurance;
        this.authenticationEnabled = authenticationEnabled;
    }

    public String getSimpleId() {
        return simpleId;
    }

    public String getEntityId() {
        return entityId;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }
    
    public boolean isAuthenticationEnabled() { return authenticationEnabled; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdpDto idpDto = (IdpDto) o;

        if (!entityId.equals(idpDto.entityId)) {
            return false;
        }
        if (!simpleId.equals(idpDto.simpleId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = simpleId.hashCode();
        result = 31 * result + entityId.hashCode();
        return result;
    }
}
