package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.net.URI;

public class EidasCycle3MatchRequestSentState extends EidasCycle0And1MatchRequestSentState {

    private static final long serialVersionUID = 8951117516881029017L;

    @JsonCreator
    public EidasCycle3MatchRequestSentState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerEntityId") final String requestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
            @JsonProperty("identityProviderEntityId") final String identityProviderEntityId,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("idpLevelOfAssurance") final LevelOfAssurance idpLevelOfAssurance,
            @JsonProperty("matchingServiceAdapterEntityId") final String matchingServiceAdapterEntityId,
            @JsonProperty("encryptedIdentityAssertion") final String encryptedIdentityAssertion,
            @JsonProperty("persistentId") final PersistentId persistentId,
            @JsonProperty("forceAuthentication") final Boolean forceAuthentication,
            @JsonProperty("countrySignedResponseContainer") final CountrySignedResponseContainer countrySignedResponseContainer) {

        super(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                identityProviderEntityId,
                relayState,
                idpLevelOfAssurance,
                matchingServiceAdapterEntityId,
                encryptedIdentityAssertion,
                persistentId,
                forceAuthentication,
                countrySignedResponseContainer
        );
    }
}
