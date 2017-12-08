package uk.gov.ida.integrationtest.hub.samlengine.builders;

import uk.gov.ida.hub.samlengine.contracts.RequestForErrorResponseFromHubDto;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.test.TestEntityIds;

import java.net.URI;

public class RequestForErrorResponseFromHubDtoBuilder {
    private String authnRequestIssuerEntityId = TestEntityIds.TEST_RP;
    private String responseId = "responseId";
    private String inResponseTo = "inResponseTo";
    private URI assertionConsumerServiceUri = URI.create("http://test-rp");
    private TransactionIdaStatus status = TransactionIdaStatus.NoAuthenticationContext;

    public static RequestForErrorResponseFromHubDtoBuilder aRequestForErrorResponseFromHubDto() {
        return new RequestForErrorResponseFromHubDtoBuilder();
    }

    public RequestForErrorResponseFromHubDtoBuilder withAuthnRequestIssuerEntityId(String authnRequestIssuerEntityId) {
        this.authnRequestIssuerEntityId = authnRequestIssuerEntityId;
        return this;
    }

    public RequestForErrorResponseFromHubDtoBuilder withResponseId(String responseId) {
        this.responseId = responseId;
        return this;
    }

    public RequestForErrorResponseFromHubDtoBuilder withInResponseTo(String inResponseTo) {
        this.inResponseTo = inResponseTo;
        return this;
    }

    public RequestForErrorResponseFromHubDtoBuilder withAssertionConsumerServiceUri(URI assertionConsumerServiceUri) {
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        return this;
    }

    public RequestForErrorResponseFromHubDtoBuilder withStatus(TransactionIdaStatus status) {
        this.status = status;
        return this;
    }

    public RequestForErrorResponseFromHubDto build() {
        return new RequestForErrorResponseFromHubDto(authnRequestIssuerEntityId, responseId, inResponseTo, assertionConsumerServiceUri, status);
    }
}
