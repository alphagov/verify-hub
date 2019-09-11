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

public class EidasCountrySelectedState extends AbstractState implements EidasCountrySelectingState, RestartJourneyState, Serializable {

    private static final long serialVersionUID = -285602589000108606L;

    @JsonProperty
    private String countryEntityId;
    @JsonProperty
    private final String relayState;
    @JsonProperty
    private List<LevelOfAssurance> levelsOfAssurance;

    @JsonCreator
    public EidasCountrySelectedState(
            @JsonProperty("countryEntityId") final String countryEntityId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerId") final String requestIssuerId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
            @JsonProperty("levelsOfAssurance") final List<LevelOfAssurance> levelsOfAssurance,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication) {
        super(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            forceAuthentication
        );

        this.relayState = relayState;
        this.countryEntityId = countryEntityId;
        this.levelsOfAssurance = levelsOfAssurance;
    }

    @Override
    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }

    public String getCountryEntityId() { return countryEntityId; }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }
}
