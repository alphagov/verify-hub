package uk.gov.ida.hub.policy.builder.domain;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.ReceivedAuthnRequest;

import java.net.URI;
import java.util.UUID;

public class ReceivedAuthnRequestBuilder {

    private String id = UUID.randomUUID().toString();
    private String issuer = "issuer_id";
    private DateTime issueInstant = DateTime.now();
    private Boolean forceAuthentication;
    private URI assertionConsumerServiceUri = null;
    private String relayState;
    private String principalIpAddress = "some-principal-ip-address";

    public static ReceivedAuthnRequestBuilder aReceivedAuthnRequest() {
        return new ReceivedAuthnRequestBuilder();
    }

    public ReceivedAuthnRequest build() {
        return new ReceivedAuthnRequest(
                id,
                issuer,
                issueInstant,
                forceAuthentication,
                assertionConsumerServiceUri,
                relayState,
                DateTime.now(),
                principalIpAddress);
    }

    public ReceivedAuthnRequestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ReceivedAuthnRequestBuilder withIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public ReceivedAuthnRequestBuilder withIssueInstant(DateTime issueInstant) {
        this.issueInstant = issueInstant;
        return this;
    }

    public ReceivedAuthnRequestBuilder withAssertionConsumerServiceUri(URI assertionConsumerServiceUri) {
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        return this;
    }

    public ReceivedAuthnRequestBuilder withRelayState(String relayState) {
        this.relayState = relayState;
        return this;
    }

    public ReceivedAuthnRequestBuilder withPrincipalIpAddress(String principalIpAddress) {
        this.principalIpAddress = principalIpAddress;
        return this;
    }
}
