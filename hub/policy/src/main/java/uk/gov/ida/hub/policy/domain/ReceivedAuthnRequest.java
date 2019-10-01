package uk.gov.ida.hub.policy.domain;

import org.joda.time.DateTime;

import java.net.URI;
import java.util.Optional;

public class ReceivedAuthnRequest {

    private String relayState;
    private DateTime receivedTime;
    private DateTime issueInstant;
    private String id;
    private String issuer;
    private Boolean forceAuthentication;
    private URI assertionConsumerServiceUri;
    private String principalIpAddress;

    @SuppressWarnings("unused")//Needed by JAXB
    private ReceivedAuthnRequest() {
    }

    public ReceivedAuthnRequest(
            String id,
            String issuer,
            DateTime issueInstant,
            Boolean forceAuthentication,
            URI assertionConsumerServiceUri,
            String relayState,
            DateTime receivedTime,
            String principalIpAddress) {

        this.id = id;
        this.issuer = issuer;
        this.issueInstant = issueInstant;
        this.forceAuthentication = forceAuthentication;
        this.assertionConsumerServiceUri = assertionConsumerServiceUri;
        this.relayState = relayState;
        this.receivedTime = receivedTime;
        this.principalIpAddress = principalIpAddress;
    }

    public Optional<String> getRelayState() {
        return Optional.ofNullable(relayState);
    }

    public DateTime getReceivedTime() {
        return receivedTime;
    }

    public String getId() {
        return id;
    }

    public String getIssuer() {
        return issuer;
    }

    public Optional<Boolean> getForceAuthentication() {
        return Optional.ofNullable(forceAuthentication);
    }

    public DateTime getIssueInstant() {
        return issueInstant;
    }

    public URI getAssertionConsumerServiceUri() {
        return assertionConsumerServiceUri;
    }

    public String getPrincipalIpAddress() {
        return principalIpAddress;
    }
}
