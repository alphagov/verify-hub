package uk.gov.ida.hub.policy.domain.state;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Set;

public class NonMatchingJourneySuccessState extends AbstractState implements ResponsePreparedState {

    @JsonProperty
    private final Optional<String> relayState;
    @JsonProperty
    private final Set<String> encryptedAssertions;

    @JsonCreator
    public NonMatchingJourneySuccessState(
            @JsonProperty("requestId") final String requestId,
            @JsonProperty("requestIssuerEntityId") final String requestIssuerEntityId,
            @JsonProperty("sessionExpiryTimestamp") final DateTime sessionExpiryTimestamp,
            @JsonProperty("assertionConsumerServiceUri") final URI assertionConsumerServiceUri,
            @JsonProperty("sessionId") final SessionId sessionId,
            @JsonProperty("transactionSupportsEidas") final boolean transactionSupportsEidas,
            @JsonProperty("relayState") final String relayState,
            @JsonProperty("encryptedAssertions") final Set<String> encryptedAssertions) {

        super(
                requestId,
                requestIssuerEntityId,
                sessionExpiryTimestamp,
                assertionConsumerServiceUri,
                sessionId,
                transactionSupportsEidas,
                null
        );

        this.relayState = Optional.fromNullable(relayState);
        this.encryptedAssertions = encryptedAssertions;
    }

    @Override
    public Optional<String> getRelayState() {
        return relayState;
    }

    public Set<String> getEncryptedAssertions() {
        return encryptedAssertions;
    }

}
