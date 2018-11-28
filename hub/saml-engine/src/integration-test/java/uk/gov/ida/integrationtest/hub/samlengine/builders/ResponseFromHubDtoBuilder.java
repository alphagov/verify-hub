package uk.gov.ida.integrationtest.hub.samlengine.builders;

import uk.gov.ida.hub.samlengine.contracts.ResponseFromHubDto;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ResponseFromHubDtoBuilder {

    private String authnRequestIssuerEntityId = UUID.randomUUID().toString();
    private String responseId = UUID.randomUUID().toString();
    private String inResponseTo = UUID.randomUUID().toString();
    private TransactionIdaStatus status = TransactionIdaStatus.Success;
    private Optional<String> assertion = Optional.empty();
    private List<String> encryptedAssertions = Collections.emptyList();
    private Optional<String> relayState = Optional.empty();
    private URI assertionConsumerServiceUri = URI.create("/default-index");

    public static ResponseFromHubDtoBuilder aResponseFromHubDto() {
        return new ResponseFromHubDtoBuilder();
    }

    public ResponseFromHubDto build() {
        return new ResponseFromHubDto(
                responseId,
                inResponseTo,
                authnRequestIssuerEntityId,
                assertion,
                encryptedAssertions,
                relayState,
                assertionConsumerServiceUri,
                status
        );
    }

    public ResponseFromHubDtoBuilder withRelayState(String relayState) {
        this.relayState = Optional.ofNullable(relayState);
        return this;
    }

    public ResponseFromHubDtoBuilder withAssertion(String assertion) {
        this.assertion = Optional.of(assertion);
        return this;
    }

    public ResponseFromHubDtoBuilder withAssertions(List<String> encryptedAssertions) {
        this.encryptedAssertions = encryptedAssertions;
        return this;
    }

    public ResponseFromHubDtoBuilder withAssertionConsumerServiceUri(URI assertionConsumerServiceUri) {
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        return this;
    }

    public ResponseFromHubDtoBuilder withAuthnRequestIssuerEntityId(String authnRequestIssuerEntityId) {
        this.authnRequestIssuerEntityId = authnRequestIssuerEntityId;
        return this;
    }

    public ResponseFromHubDtoBuilder withInResponseTo(String inResponseTo) {
        this.inResponseTo = inResponseTo;
        return this;
    }

    public ResponseFromHubDtoBuilder withStatus(TransactionIdaStatus status) {
        this.status = status;
        return this;
    }
}
