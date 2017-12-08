package uk.gov.ida.hub.policy.contracts;

import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;

import java.net.URI;

public class RequestForErrorResponseFromHubDto {
    private String authnRequestIssuerEntityId;
    private String responseId;
    private String inResponseTo;
    private URI assertionConsumerServiceUri;
    private TransactionIdaStatus status;

    public RequestForErrorResponseFromHubDto(String authnRequestIssuerEntityId, String responseId, String inResponseTo, URI assertionConsumerServiceUri, TransactionIdaStatus status) {
        this.authnRequestIssuerEntityId = authnRequestIssuerEntityId;
        this.responseId = responseId;
        this.inResponseTo = inResponseTo;
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        this.status = status;
    }

    private RequestForErrorResponseFromHubDto() {}


    public String getAuthnRequestIssuerEntityId() {
        return authnRequestIssuerEntityId;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getInResponseTo() {
        return inResponseTo;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public TransactionIdaStatus getStatus() {
        return status;
    }
}
