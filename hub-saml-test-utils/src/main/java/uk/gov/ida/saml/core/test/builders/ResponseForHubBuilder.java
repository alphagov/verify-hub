package uk.gov.ida.saml.core.test.builders;

import org.joda.time.DateTime;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.domain.IdpIdaStatus;
import uk.gov.ida.saml.core.domain.InboundResponseFromIdp;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;
import uk.gov.ida.saml.core.test.TestEntityIds;

import java.net.URI;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class ResponseForHubBuilder {

    private String responseId = "response-id";
    private String inResponseTo = "request-id";
    private String issuerId = "issuer-id";
    private DateTime issueInstant = DateTime.now();
    private TransactionIdaStatus transactionIdpStatus = TransactionIdaStatus.Success;
    private IdpIdaStatus idpIdaStatus = IdpIdaStatus.success();
    private Optional<Signature> signature = null;
    private Optional<PassthroughAssertion> authnStatementAssertion = empty();
    private Optional<PassthroughAssertion> matchingDatasetAssertion = empty();
    private Optional<String> matchingServiceAssertion = empty();


    public static ResponseForHubBuilder anAuthnResponse() {
        return new ResponseForHubBuilder();
    }

    public InboundResponseFromIdp buildInboundFromIdp() {
        return new InboundResponseFromIdp(
                responseId,
                inResponseTo,
                issuerId,
                issueInstant,
                idpIdaStatus,
                signature,
                matchingDatasetAssertion,
                null,
                authnStatementAssertion
        );
    }

    public InboundResponseFromIdp buildSuccessFromIdp() {
        return new InboundResponseFromIdp(
                responseId,
                inResponseTo,
                issuerId,
                issueInstant,
                idpIdaStatus,
                signature,
                matchingDatasetAssertion,
                null,
                authnStatementAssertion
        );
    }

    public OutboundResponseFromHub buildOutboundResponseFromHub() {
        return new OutboundResponseFromHub(
                responseId,
                inResponseTo,
                TestEntityIds.HUB_ENTITY_ID,
                issueInstant,
                transactionIdpStatus,
                matchingServiceAssertion,
                URI.create("blah"));
    }


    public ResponseForHubBuilder withResponseId(String responseId) {
        this.responseId = responseId;
        return this;
    }

    public ResponseForHubBuilder withInResponseTo(String inResponseTo) {
        this.inResponseTo = inResponseTo;
        return this;
    }

    public ResponseForHubBuilder withIssuerId(String issuerId) {
        this.issuerId = issuerId;
        return this;
    }

    public ResponseForHubBuilder withIssueInstant(DateTime issueInstant) {
        this.issueInstant = issueInstant;
        return this;
    }

    public ResponseForHubBuilder withIdpIdaStatus(IdpIdaStatus status) {
        this.idpIdaStatus = status;
        return this;
    }

    public ResponseForHubBuilder withTransactionIdaStatus(TransactionIdaStatus status) {
        this.transactionIdpStatus = status;
        return this;
    }

    public ResponseForHubBuilder withSignature(Signature signature) {
        this.signature = ofNullable(signature);
        return this;
    }

    public ResponseForHubBuilder withAuthnStatementAssertion(PassthroughAssertion authnStatementAssertion) {
        this.authnStatementAssertion = ofNullable(authnStatementAssertion);
        return this;
    }

    public ResponseForHubBuilder withMatchingDatasetAssertion(PassthroughAssertion matchingDatasetAssertion) {
        this.matchingDatasetAssertion = ofNullable(matchingDatasetAssertion);
        return this;
    }

    public ResponseForHubBuilder withMatchingServiceAssertion(String matchingServiceAssertion) {
        this.matchingServiceAssertion = ofNullable(matchingServiceAssertion);
        return this;
    }
}
