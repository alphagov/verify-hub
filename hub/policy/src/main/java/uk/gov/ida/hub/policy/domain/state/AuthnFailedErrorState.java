package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.List;

public class AuthnFailedErrorState extends AbstractAuthnFailedErrorState implements IdpSelectingState {

    private static final long serialVersionUID = 8101005936409595481L;

    @JsonProperty
    private String idpEntityId;

    @JsonCreator
    public AuthnFailedErrorState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("authnRequestIssuerEntityId") final String authnRequestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("idpEntityId") final String idpEntityId,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
            @JsonProperty("levelOfAssurance")final List<LevelOfAssurance> levelsOfAssurance) {

        super(
                requestId,
                authnRequestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                relayState,
                sessionId,
                transactionSupportsEidas,
                levelsOfAssurance,
                forceAuthentication
        );
        this.idpEntityId = idpEntityId;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }
}
