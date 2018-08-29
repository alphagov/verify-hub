package uk.gov.ida.hub.policy.domain.state;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.List;

public class CountryAuthnFailedErrorState extends AbstractAuthnFailedErrorState implements CountrySelectingState, EidasUnsuccessfulJourneyState {

    private static final long serialVersionUID = -6087079428518232137L;

    private String countryEntityId;
    private List<LevelOfAssurance> levelsOfAssurance;

    public CountryAuthnFailedErrorState(
            String requestId,
            String authnRequestIssuerEntityId,
            DateTime sessionExpiryTimestamp,
            URI assertionConsumerServiceUri,
            String relayState,
            SessionId sessionId,
            String countryEntityId,
            List<LevelOfAssurance> levelsOfAssurance) {

        super(requestId, authnRequestIssuerEntityId, sessionExpiryTimestamp, assertionConsumerServiceUri, relayState, sessionId, true);

        this.countryEntityId = countryEntityId;
        this.levelsOfAssurance = levelsOfAssurance;
    }

    public String getCountryEntityId() {
        return countryEntityId;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return levelsOfAssurance;
    }
}
