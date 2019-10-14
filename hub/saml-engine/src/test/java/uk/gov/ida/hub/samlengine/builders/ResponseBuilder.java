package uk.gov.ida.hub.samlengine.builders;

import org.joda.time.DateTime;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;

// qTODO - is this a builder?
public class ResponseBuilder<T extends ResponseBuilder> {

    protected String responseId = null;
    protected DateTime issueInstant = null;
    protected String inResponseTo = "request-id";
    protected String issuerId = null;
    protected MatchingServiceIdaStatus status = null;

    public T withResponseId(String responseId) {
        this.responseId = responseId;
        return (T) this;
    }

    public T withIssueInstant(DateTime issueInstant) {
        this.issueInstant = issueInstant;
        return (T) this;
    }

    public T withInResponseTo(String inResponseTo) {
        this.inResponseTo = inResponseTo;
        return (T) this;
    }

    public T withIssuerId(String issuer) {
        this.issuerId = issuer;
        return (T) this;
    }

    public T withStatus(MatchingServiceIdaStatus status) {
        this.status = status;
        return (T) this;
    }
}
