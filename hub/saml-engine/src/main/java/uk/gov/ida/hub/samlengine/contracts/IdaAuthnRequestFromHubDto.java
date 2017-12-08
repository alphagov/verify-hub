package uk.gov.ida.hub.samlengine.contracts;

import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.AuthnContext;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class IdaAuthnRequestFromHubDto {
    private String id;
    private Optional<Boolean> forceAuthentication;
    private DateTime sessionExpiryTimestamp;
    private String idpEntityId;
    private List<AuthnContext> levelsOfAssurance;
    private Boolean useExactComparisonType;
    private URI overriddenSsoUrl;

    @SuppressWarnings("unused") // needed for JAXB
    private IdaAuthnRequestFromHubDto(){}

    public IdaAuthnRequestFromHubDto(
        String id,
        List<AuthnContext> levelsOfAssurance,
        Optional<Boolean> forceAuthentication,
        DateTime sessionExpiryTimestamp,
        String idpEntityId,
        Boolean useExactComparisonType,
        URI overriddenSsoUrl) {
        this(id, levelsOfAssurance, forceAuthentication, sessionExpiryTimestamp, idpEntityId, useExactComparisonType);
        this.overriddenSsoUrl = overriddenSsoUrl;
    }

    public IdaAuthnRequestFromHubDto(
            String id,
            List<AuthnContext> levelsOfAssurance,
            Optional<Boolean> forceAuthentication,
            DateTime sessionExpiryTimestamp,
            String idpEntityId,
            Boolean useExactComparisonType) {
        this.id = id;
        this.levelsOfAssurance = levelsOfAssurance;
        this.forceAuthentication = forceAuthentication;
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        this.idpEntityId = idpEntityId;
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
        return idpEntityId;
    }

    public List<AuthnContext> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }

    public Boolean getUseExactComparisonType() {
        return useExactComparisonType;
    }

    public URI getoverriddenSsoUrl() { return overriddenSsoUrl; }
}
