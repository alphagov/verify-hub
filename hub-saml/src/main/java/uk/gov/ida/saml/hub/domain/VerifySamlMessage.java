package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;

import java.net.URI;

public abstract class VerifySamlMessage extends VerifyMessage {

    private URI destination;

    protected VerifySamlMessage() {
    }

    public VerifySamlMessage(String id, String issuer, DateTime issueInstant, URI destination) {
        super(id, issuer, issueInstant);
        this.destination = destination;
    }

    public URI getDestination() {
        return destination;
    }
}
