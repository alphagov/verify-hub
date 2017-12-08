package uk.gov.ida.hub.samlengine.contracts;

import uk.gov.ida.saml.core.domain.TransactionIdaStatus;

import java.net.URI;
import java.util.Optional;

public class ResponseFromHubDto {

    private String authnRequestIssuerEntityId;
    private String responseId;
    private String inResponseTo;
    private TransactionIdaStatus status;
    private Optional<String> matchingServiceAssertion;
    private Optional<String> relayState;
    private URI assertionConsumerServiceUri;

    @SuppressWarnings("unused") // needed for JAXB
    private ResponseFromHubDto() {
    }

    public ResponseFromHubDto(
            String responseId,
            String inResponseTo,
            String authnRequestIssuerEntityId,
            Optional<String> matchingServiceAssertion,
            Optional<String> relayState,
            URI assertionConsumerServiceUri,
            TransactionIdaStatus status) {

        this.authnRequestIssuerEntityId = authnRequestIssuerEntityId;
        this.responseId = responseId;
        this.inResponseTo = inResponseTo;
        this.matchingServiceAssertion = matchingServiceAssertion;
        this.relayState = relayState;
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        this.status = status;
    }

    public String getAuthnRequestIssuerEntityId() {
        return authnRequestIssuerEntityId;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getInResponseTo() {
        return inResponseTo;
    }

    public Optional<String> getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public Optional<String> getRelayState() {
        return relayState;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public TransactionIdaStatus getStatus() {
        return status;
    }

}

