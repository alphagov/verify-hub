package uk.gov.ida.hub.policy.builder.domain;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;

import java.net.URI;
import java.util.UUID;

import static java.util.Collections.emptyList;

public class ResponseFromHubBuilder {

    private String authnRequestIssuerEntityId = UUID.randomUUID().toString();
    private String responseId = UUID.randomUUID().toString();
    private String inResponseTo = UUID.randomUUID().toString();
    private uk.gov.ida.hub.policy.domain.TransactionIdaStatus status = TransactionIdaStatus.Success;
    private Optional<String> relayState = Optional.absent();
    private URI assertionConsumerServiceUri = URI.create("/default-index");

    public static ResponseFromHubBuilder aResponseFromHubDto() {
        return new ResponseFromHubBuilder();
    }

    public ResponseFromHubBuilder withRelayState(String relayState) {
        this.relayState = Optional.fromNullable(relayState);
        return this;
    }

    public ResponseFromHub build() {
        return new ResponseFromHub(
                responseId,
                inResponseTo,
                authnRequestIssuerEntityId,
                emptyList(),
                relayState,
                assertionConsumerServiceUri,
                status
        );
    }
}

