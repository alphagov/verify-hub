package uk.gov.ida.hub.policy.builder.domain;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHub;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuthnRequestFromHubBuilder {
    private String id = UUID.randomUUID().toString();
    private List<LevelOfAssurance> levelsOfAssurance = Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
    private String recipientEntityId = UUID.randomUUID().toString();
    private Optional<Boolean> forceAuthentication = Optional.empty();
    private DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(5);
    private Boolean useExactComparisonType = false;
    private boolean registering = false;
    private URI overriddenSsoUri;

    public static AuthnRequestFromHubBuilder anAuthnRequestFromHub() {
        return new AuthnRequestFromHubBuilder();
    }

    private AuthnRequestFromHubBuilder() {}

    public AuthnRequestFromHubBuilder withSsoUrl(URI overriddenSsoUri) {
        this.overriddenSsoUri = overriddenSsoUri;
        return this;
    }

    public AuthnRequestFromHub build() {
        return new AuthnRequestFromHub(
                id,
                levelsOfAssurance,
                useExactComparisonType,
                recipientEntityId,
                forceAuthentication,
                sessionExpiryTimestamp,
                registering,
                overriddenSsoUri);
    }
}
