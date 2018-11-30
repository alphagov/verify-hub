package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

public class IdpSelectedState extends AbstractState implements IdpSelectingState, RestartJourneyState, Serializable {

    private static final long serialVersionUID = -2851353851977677375L;

    private final String idpEntityId;

    private Boolean useExactComparisonType;
    private List<LevelOfAssurance> levelsOfAssurance;
    private final String relayState;
    private final boolean registering;
    private final LevelOfAssurance requestedLoa;
    private final List<String> availableIdentityProviders;

    public IdpSelectedState(
            String requestId,
            String idpEntityId,
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

        super(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
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

    public Boolean getUseExactComparisonType() {
        return useExactComparisonType;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }
}
