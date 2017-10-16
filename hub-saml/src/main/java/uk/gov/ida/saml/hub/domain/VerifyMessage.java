package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;

public abstract class VerifyMessage {

    private String id;
    private String issuer;
    private DateTime issueInstant;

    protected VerifyMessage() {
    }

    public VerifyMessage(String id, String issuer, DateTime issueInstant) {
        this.id = id;
        this.issuer = issuer;
        this.issueInstant = issueInstant;
    }

    public String getId() {
        return id;
    }

    public String getIssuer() {
        return issuer;
    }

    public DateTime getIssueInstant() {
        return issueInstant;
    }
}
