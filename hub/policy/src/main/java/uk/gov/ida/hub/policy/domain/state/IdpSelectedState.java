package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class IdpSelectedState extends AbstractState implements IdpSelectingState, Serializable {

    private final String idpEntityId;
    private final String matchingServiceEntityId;

    private Boolean useExactComparisonType = false;
    private List<LevelOfAssurance> levelsOfAssurance = Collections.EMPTY_LIST;
    private final Optional<Boolean> forceAuthentication;
    private final Optional<String> relayState;
    private final boolean registering;
    private final LevelOfAssurance requestedLoa;
    private final List<String> availableIdentityProviders;

    public IdpSelectedState(
            String requestId,
            String idpEntityId,
            String matchingServiceEntityId,
            List<LevelOfAssurance> levelsOfAssurance,
            Boolean useExactComparisonType,
            Optional<Boolean> forceAuthentication,
            URI assertionConsumerServiceUri,
            String requestIssuerId,
            Optional<String> relayState,
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
        return forceAuthentication;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public List<String> getAvailableIdentityProviderEntityIds() {
        return availableIdentityProviders;
    }

    public boolean registering() {
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
