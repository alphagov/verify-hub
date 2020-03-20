package uk.gov.ida.hub.config.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.List;

public class IdpDto {

    private final String simpleId;
    private final String entityId;
    private final String provideRegistrationUntil;
    private final List<LevelOfAssurance> levelsOfAssurance;
    private final boolean authenticationEnabled;
    private final boolean temporarilyUnavailable;

    @JsonCreator
    public IdpDto(
            @JsonProperty("simpleId") String simpleId,
            @JsonProperty("entityId") String entityId,
            @JsonProperty("provideRegistrationUntil") String provideRegistrationUntil,
            @JsonProperty("supportedLevelsOfAssurance") List<LevelOfAssurance> levelsOfAssurance,
            @JsonProperty("authenticationEnabled") boolean authenticationEnabled,
            @JsonProperty("temporarilyUnavailable") boolean temporarilyUnavailable) {
        this.simpleId = simpleId;
        this.entityId = entityId;
        this.provideRegistrationUntil = provideRegistrationUntil;
        this.levelsOfAssurance = levelsOfAssurance;
        this.authenticationEnabled = authenticationEnabled;
        this.temporarilyUnavailable = temporarilyUnavailable;
    }

    public String getSimpleId() {
        return simpleId;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getProvideRegistrationUntil() {
        return provideRegistrationUntil;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }

    public boolean isAuthenticationEnabled() { return authenticationEnabled; }

    public boolean isTemporarilyUnavailable() { return temporarilyUnavailable; }

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
