package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.List;

public class EidasAuthnFailedErrorState extends AbstractAuthnFailedErrorState implements EidasCountrySelectingState, RestartJourneyState {

    private static final long serialVersionUID = -6087079428518232137L;

    @JsonProperty
    private String countryEntityId;
    @JsonProperty
    private List<LevelOfAssurance> levelsOfAssurance;

    @JsonCreator
    public EidasAuthnFailedErrorState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("authnRequestIssuerEntityId") final String authnRequestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("countryEntityId") final String countryEntityId,
            @JsonProperty("levelsOfAssurance") final List<LevelOfAssurance> levelsOfAssurance,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication) {

        super(
            requestId,
            authnRequestIssuerEntityId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            relayState,
            sessionId,
            true,
            levelsOfAssurance,
            forceAuthentication
        );

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
