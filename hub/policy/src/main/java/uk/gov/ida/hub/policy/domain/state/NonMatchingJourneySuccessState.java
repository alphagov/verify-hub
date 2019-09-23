package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.saml.core.domain.EidasCountrySignedResponseWithEncryptedKeys;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public class NonMatchingJourneySuccessState extends AbstractState implements ResponsePreparedState {

    @JsonProperty
    private final Optional<String> relayState;
    @JsonProperty
    private final Set<String> encryptedAssertions;
    @JsonProperty
    private final Optional<EidasCountrySignedResponseWithEncryptedKeys> countrySignedResponseWithEncryptedKeys;

    @JsonCreator
    public NonMatchingJourneySuccessState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerEntityId") final String requestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("encryptedAssertions") final Set<String> encryptedAssertions,
            @JsonProperty("countrySignedResponseWithEncryptedKeys") final EidasCountrySignedResponseWithEncryptedKeys countrySignedResponseWithEncryptedKeys
    ) {

        super(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                null
        );

        this.relayState = Optional.ofNullable(relayState);
        this.encryptedAssertions = encryptedAssertions;
        this.countrySignedResponseWithEncryptedKeys = Optional.ofNullable(countrySignedResponseWithEncryptedKeys);
    }

    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }

    public Set<String> getEncryptedAssertions() {
        return encryptedAssertions;
    }

    public Optional<EidasCountrySignedResponseWithEncryptedKeys> getCountrySignedResponseWithEncryptedKeys() { return countrySignedResponseWithEncryptedKeys; }

}
