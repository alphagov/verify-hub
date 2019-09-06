package uk.gov.ida.hub.policy.domain;

import uk.gov.ida.common.shared.security.IdGenerator;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class ResponseFromHubFactory {

    private final IdGenerator idGenerator;

    @Inject
    public ResponseFromHubFactory(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public ResponseFromHub createSuccessResponseFromHub(
            String inResponseTo,
            String matchingServiceAssertion,
            Optional<String> relayState,
            String authnRequestIssuerEntityId,
            URI assertionConsumerServiceUri) {

        return new ResponseFromHub(
                idGenerator.getId(),
                inResponseTo,
                authnRequestIssuerEntityId,
                singletonList(matchingServiceAssertion),
                relayState,
                assertionConsumerServiceUri,
                TransactionIdaStatus.Success
        );
    }

    public ResponseFromHub createNoAuthnContextResponseFromHub(
            String inResponseTo,
            Optional<String> relayState,
            String authnRequestIssuerEntityId,
            URI assertionConsumerServiceUri) {

        return new ResponseFromHub(
                idGenerator.getId(),
                inResponseTo,
                authnRequestIssuerEntityId,
                emptyList(),
                relayState,
                assertionConsumerServiceUri,
                TransactionIdaStatus.NoAuthenticationContext
        );
    }

    public ResponseFromHub createNoMatchResponseFromHub(
            String inResponseTo,
            Optional<String> relayState,
            String authnRequestIssuerEntityId,
            URI assertionConsumerServiceUri) {

        return new ResponseFromHub(
                idGenerator.getId(),
                inResponseTo,
                authnRequestIssuerEntityId,
                emptyList(),
                relayState,
                assertionConsumerServiceUri,
                TransactionIdaStatus.NoMatchingServiceMatchFromHub
        );
    }

    public ResponseFromHub createAuthnFailedResponseFromHub(
            String inResponseTo,
            Optional<String> relayState,
            String authnRequestIssuerEntityId,
            URI assertionConsumerServiceUri) {

        return new ResponseFromHub(
                idGenerator.getId(),
                inResponseTo,
                authnRequestIssuerEntityId,
                emptyList(),
                relayState,
                assertionConsumerServiceUri,
                TransactionIdaStatus.AuthenticationFailed
        );
    }

    public ResponseFromHub createRequesterErrorResponseFromHub(
            String requestId,
            Optional<String> relayState,
            String requestIssuerId,
            URI assertionConsumerServiceUri) {

        return new ResponseFromHub(
            idGenerator.getId(),
            requestId,
            requestIssuerId,
            emptyList(),
            relayState,
            assertionConsumerServiceUri,
            TransactionIdaStatus.RequesterError
        );
    }

    public ResponseFromHub createNonMatchingSuccessResponseFromHub(
            String requestId,
            Optional<String> relayState,
            String requestIssuerEntityId,
            List<String> encryptedAssertions,
            URI assertionConsumerServiceUri) {

        return new ResponseFromHub(
            idGenerator.getId(),
            requestId,
            requestIssuerEntityId,
            encryptedAssertions,
            relayState,
            assertionConsumerServiceUri,
            TransactionIdaStatus.Success
        );
    }
}