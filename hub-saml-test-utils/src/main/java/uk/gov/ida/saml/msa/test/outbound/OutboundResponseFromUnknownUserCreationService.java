package uk.gov.ida.saml.msa.test.outbound;

import org.joda.time.DateTime;

import com.google.common.base.Optional;

import uk.gov.ida.saml.core.domain.IdaMatchingServiceResponse;
import uk.gov.ida.saml.msa.test.domain.MatchingServiceAssertion;
import uk.gov.ida.saml.msa.test.domain.UnknownUserCreationIdaStatus;

public class OutboundResponseFromUnknownUserCreationService extends IdaMatchingServiceResponse {
    private final UnknownUserCreationIdaStatus status;
    private final Optional<MatchingServiceAssertion> matchingServiceAssertion;

    public OutboundResponseFromUnknownUserCreationService(
            String responseId,
            String inResponseTo,
            String issuer,
            DateTime issueInstant,
            UnknownUserCreationIdaStatus status,
            Optional<MatchingServiceAssertion> matchingServiceAssertion) {
        super(responseId, inResponseTo, issuer, issueInstant);
        this.status = status;
        this.matchingServiceAssertion = matchingServiceAssertion;
    }

    public Optional<MatchingServiceAssertion> getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public UnknownUserCreationIdaStatus getStatus() {
        return status;
    }
}
