package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.StandardToStringStyle;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;

import java.net.URI;
import java.util.Objects;

public final class EidasAwaitingCycle3DataState extends AbstractAwaitingCycle3DataState {

    private static final long serialVersionUID = -9056285913241958733L;

    private final String encryptedIdentityAssertion;

    public EidasAwaitingCycle3DataState(
        final String requestId,
        final String requestIssuerId,
        final DateTime sessionExpiryTimestamp,
        final URI assertionConsumerServiceUri,
        final SessionId sessionId,
        final boolean transactionSupportsEidas,
        final String identityProviderEntityId,
        final String matchingServiceAdapterEntityId,
        final Optional<String> relayState,
        final PersistentId persistentId,
        final LevelOfAssurance levelOfAssurance,
        final String encryptedIdentityAssertion,
        final Boolean forceAuthentication) {

        super(
            requestId,
            requestIssuerId,
            sessionExpiryTimestamp,
            assertionConsumerServiceUri,
            sessionId,
            transactionSupportsEidas,
            identityProviderEntityId,
            matchingServiceAdapterEntityId,
            relayState,
            persistentId,
            levelOfAssurance,
            forceAuthentication
        );

        this.encryptedIdentityAssertion = encryptedIdentityAssertion;
    }

    public String getEncryptedIdentityAssertion() {
        return encryptedIdentityAssertion;
    }
}
