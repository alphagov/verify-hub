package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Optional.fromNullable;

public class SessionStartedStateFactory {

    private final IdentityProvidersConfigProxy identityProvidersConfigProxy;

    @Inject
    public SessionStartedStateFactory(IdentityProvidersConfigProxy identityProvidersConfigProxy) {
        this.identityProvidersConfigProxy = identityProvidersConfigProxy;
    }

    public SessionStartedState build(
            String authnRequestId,
            URI assertionConsumerServiceUri,
            String requestIssuerId,
            Optional<String> relayState,
            Optional<Boolean> forceAuthentication,
            DateTime sessionExpiryTimestamp,
            SessionId sessionId,
            boolean transactionSupportsEidas) {

        List<String> availableIdpEntities = identityProvidersConfigProxy.getEnabledIdentityProviders(
                fromNullable(requestIssuerId));

        return new SessionStartedState(
                authnRequestId,
                relayState,
                requestIssuerId,
                assertionConsumerServiceUri,
                forceAuthentication,
                availableIdpEntities,
                sessionExpiryTimestamp,
                sessionId,
                transactionSupportsEidas
        );
    }
}


