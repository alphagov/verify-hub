package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.List;

public class AuthnRequestFromHub {

    private String id;
    private List<LevelOfAssurance> levelsOfAssurance;
    private Boolean useExactComparisonType;
    private String recipientEntityId;
    private Optional<Boolean> forceAuthentication;
    private DateTime sessionExpiryTimestamp;
    private boolean registering;
    private URI overriddenSsoUrl;


    @SuppressWarnings("unused")//Needed by JAXB
    private AuthnRequestFromHub() {
    }

    public AuthnRequestFromHub(
            String id,
            List<LevelOfAssurance> levelsOfAssurance,
            Boolean useExactComparisonType,
            String recipientEntityId,
            Optional<Boolean> forceAuthentication,
            DateTime sessionExpiryTimestamp,
            boolean registering,
            URI overriddenSsoUrl) {

        this.id = id;
        this.levelsOfAssurance = levelsOfAssurance;
        this.useExactComparisonType = useExactComparisonType;
        this.recipientEntityId = recipientEntityId;
        this.forceAuthentication = forceAuthentication;
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        this.registering = registering;
        this.overriddenSsoUrl = overriddenSsoUrl;
    }

    public String getId() {
        return id;
    }

    public String getRecipientEntityId() {
        return recipientEntityId;
    }

    public Optional<Boolean> getForceAuthentication() {
        return forceAuthentication;
    }

    public DateTime getSessionExpiryTimestamp() {
        return sessionExpiryTimestamp;
    }

    public boolean getRegistering() {
        return registering;
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
