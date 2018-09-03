package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

public class CountrySelectedState extends AbstractState implements CountrySelectingState, Serializable {

    private static final long serialVersionUID = -285602589000108606L;

    // TODO: Record matching service entity id
    private String countryEntityId;
    private final Optional<String> relayState;
    private List<LevelOfAssurance> levelsOfAssurance;

    public CountrySelectedState(String countryEntityId,
                                Optional<String> relayState,
                                String requestId,
                                String requestIssuerId,
                                DateTime sessionExpiryTimestamp,
                                URI assertionConsumerServiceUri,
                                SessionId sessionId,
                                boolean transactionSupportsEidas,
                                List<LevelOfAssurance> levelsOfAssurance) {
        super(requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas);
        this.relayState = relayState;
        this.countryEntityId = countryEntityId;
        this.levelsOfAssurance = levelsOfAssurance;
    }

    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }

    public String getCountryEntityId() { return countryEntityId; }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }
}
