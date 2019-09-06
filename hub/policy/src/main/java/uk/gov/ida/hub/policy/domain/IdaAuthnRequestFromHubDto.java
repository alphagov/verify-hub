package uk.gov.ida.hub.policy.domain;

import org.joda.time.DateTime;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class IdaAuthnRequestFromHubDto {
    private String id;
    private Optional<Boolean> forceAuthentication;
    private DateTime sessionExpiryTimestamp;
    private String idpEntityId;
    private List<LevelOfAssurance> levelsOfAssurance;
    private Boolean useExactComparisonType;
    private URI overriddenSsoUrl;

    @SuppressWarnings("unused") // needed for JAXB
    private IdaAuthnRequestFromHubDto(){}

    public IdaAuthnRequestFromHubDto(
        String id,
        Optional<Boolean> forceAuthentication,
        DateTime sessionExpiryTimestamp,
        String idpEntityId,
        List<LevelOfAssurance> levelsOfAssurance,
        Boolean useExactComparisonType,
        URI overriddenSsoUrl) {
        this(id, forceAuthentication, sessionExpiryTimestamp, idpEntityId, levelsOfAssurance, useExactComparisonType);
        this.overriddenSsoUrl = overriddenSsoUrl;
    }

    public IdaAuthnRequestFromHubDto(
            String id,
            Optional<Boolean> forceAuthentication,
            DateTime sessionExpiryTimestamp,
            String idpEntityId,
            List<LevelOfAssurance> levelsOfAssurance,
            Boolean useExactComparisonType) {
        this.id = id;
        this.forceAuthentication = forceAuthentication;
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        this.idpEntityId = idpEntityId;
        this.levelsOfAssurance = levelsOfAssurance;
        this.useExactComparisonType = useExactComparisonType;
        this.overriddenSsoUrl = null;
    }

    public String getId() {
        return id;
    }

    public Optional<Boolean> getForceAuthentication() {
        return forceAuthentication;
    }

    public DateTime getSessionExpiryTimestamp() {
        return sessionExpiryTimestamp;
    }

    public String getIdpEntityId() {
        return  idpEntityId;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }

    public Boolean getUseExactComparisonType() {
        return useExactComparisonType;
    }

    public URI getOverriddenSsoUrl() {
        return overriddenSsoUrl;
    }
}
