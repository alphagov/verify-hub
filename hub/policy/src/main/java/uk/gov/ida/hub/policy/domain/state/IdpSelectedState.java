package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class IdpSelectedState extends AbstractState implements IdpSelectingState, RestartJourneyState, Serializable {

    private static final long serialVersionUID = -2851353851977677375L;

    @JsonProperty
    private final String idpEntityId;

    @JsonProperty
    private Boolean useExactComparisonType;
    @JsonProperty
    private List<LevelOfAssurance> levelsOfAssurance;
    @JsonProperty
    private final String relayState;
    @JsonProperty
    private final boolean registering;
    @JsonProperty
    private final LevelOfAssurance requestedLoa;
    @JsonProperty
    private final List<String> availableIdentityProviders;

    @JsonCreator
    public IdpSelectedState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("idpEntityId") final String idpEntityId,
            @JsonProperty("levelsOfAssurance") final List<LevelOfAssurance> levelsOfAssurance,
            @JsonProperty("useExactComparisonType") final Boolean useExactComparisonType,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("registering") final boolean registering,
            @JsonProperty("requestedLoa") final LevelOfAssurance requestedLoa,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("availableIdentityProviders") final List<String> availableIdentityProviders)
    {

        super(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            forceAuthentication
        );

        this.idpEntityId = idpEntityId;
        this.levelsOfAssurance = levelsOfAssurance;
        this.useExactComparisonType = useExactComparisonType;
        this.relayState = relayState;
        this.registering = registering;
        this.requestedLoa = requestedLoa;
        this.availableIdentityProviders = availableIdentityProviders;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }

    public List<String> getAvailableIdentityProviders() {
        return availableIdentityProviders;
    }

    public boolean isRegistering() {
        return registering;
    }

    public LevelOfAssurance getRequestedLoa() {
        return requestedLoa;
    }

    public Boolean getUseExactComparisonType() {
        return useExactComparisonType;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }

}
