package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

public class IdpSelectedState extends AbstractState implements IdpSelectingState, Serializable {

    private final String idpEntityId;
    private final String matchingServiceEntityId;

    private Boolean useExactComparisonType;
    private List<LevelOfAssurance> levelsOfAssurance;
    private final Boolean forceAuthentication;
    private final String relayState;
    private final boolean registering;
    private final LevelOfAssurance requestedLoa;
    private final List<String> availableIdentityProviders;

    public IdpSelectedState(
            String requestId,
            String idpEntityId,
            String matchingServiceEntityId,
            List<LevelOfAssurance> levelsOfAssurance,
            Boolean useExactComparisonType,
            Boolean forceAuthentication,
            URI assertionConsumerServiceUri,
            String requestIssuerId,
            String relayState,
            DateTime sessionExpiryTimestamp,
            boolean registering,
            LevelOfAssurance requestedLoa,
            SessionId sessionId,
            List<String> availableIdentityProviders,
            boolean transactionSupportsEidas) {

        super(requestId, requestIssuerId, sessionExpiryTimestamp, assertionConsumerServiceUri, sessionId, transactionSupportsEidas);

        this.idpEntityId = idpEntityId;
        this.matchingServiceEntityId = matchingServiceEntityId;
        this.levelsOfAssurance = levelsOfAssurance;
        this.useExactComparisonType = useExactComparisonType;
        this.forceAuthentication = forceAuthentication;
        this.relayState = relayState;
        this.registering = registering;
        this.requestedLoa = requestedLoa;
        this.availableIdentityProviders = availableIdentityProviders;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public Optional<Boolean> getForceAuthentication() {
        return Optional.fromNullable(forceAuthentication);
    }

    public Optional<String> getRelayState() {
        return Optional.fromNullable(relayState);
    }

    public List<String> getAvailableIdentityProviderEntityIds() {
        return availableIdentityProviders;
    }

    public boolean isRegistering() {
        return registering;
    }

    public LevelOfAssurance getRequestedLoa() {
        return requestedLoa;
    }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }

    public Boolean getUseExactComparisonType() {
        return useExactComparisonType;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }
}
