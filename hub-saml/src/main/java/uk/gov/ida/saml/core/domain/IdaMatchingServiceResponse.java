package uk.gov.ida.saml.core.domain;

import org.joda.time.DateTime;

public abstract class IdaMatchingServiceResponse extends IdaMessage implements IdaResponse {

    private String inResponseTo;

    protected IdaMatchingServiceResponse() {
    }

    public IdaMatchingServiceResponse(String responseId, String inResponseTo, String issuer, DateTime issueInstant) {
        super(responseId, issuer, issueInstant);
        this.inResponseTo = inResponseTo;
    }

    public String getInResponseTo() {
        return inResponseTo;
    }
}
