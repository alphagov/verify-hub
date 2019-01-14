package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.io.Serializable;
import java.net.URI;

public class EidasUserAccountCreationRequestSentState extends EidasMatchRequestSentState implements Serializable {

    private static final long serialVersionUID = -3124130170426481847L;

    @JsonCreator
    public EidasUserAccountCreationRequestSentState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerEntityId") final String requestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("idpLevelOfAssurance") final LevelOfAssurance idpLevelOfAssurance,
            @JsonProperty("matchingServiceAdapterEntityId") final String matchingServiceAdapterEntityId,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication) {

        super(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                true,
                identityProviderEntityId,
                relayState,
                idpLevelOfAssurance,
                matchingServiceAdapterEntityId,
                forceAuthentication
        );
    }
}
