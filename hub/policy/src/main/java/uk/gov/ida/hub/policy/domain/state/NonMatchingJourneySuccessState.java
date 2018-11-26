package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.AbstractState;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Set;

public class NonMatchingJourneySuccessState extends AbstractState implements ResponsePreparedState {

    private final Optional<String> relayState;
    private final Set<String> encryptedAssertions;

    public NonMatchingJourneySuccessState(
        final String requestId,
        final String requestIssuerEntityId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final Optional<String> relayState,
        final Set<String> encryptedAssertions
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

        this.relayState = relayState;
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
