package uk.gov.ida.saml.msa.test.outbound;

import org.joda.time.DateTime;

import com.google.common.base.Optional;

import uk.gov.ida.saml.core.domain.IdaMatchingServiceResponse;
import uk.gov.ida.saml.core.domain.MatchingServiceIdaStatus;
import uk.gov.ida.saml.msa.test.domain.MatchingServiceAssertion;

public class OutboundResponseFromMatchingService extends IdaMatchingServiceResponse {
    private Optional<MatchingServiceAssertion> matchingServiceAssertion;
    private MatchingServiceIdaStatus status;

    public OutboundResponseFromMatchingService(
            String responseId,
            String inResponseTo,
            String issuer,
            DateTime issueInstant,
            MatchingServiceIdaStatus status,
            Optional<MatchingServiceAssertion> matchingServiceAssertion) {

        super(responseId, inResponseTo, issuer, issueInstant);

        this.matchingServiceAssertion = matchingServiceAssertion;
        this.status = status;
    }

    public Optional<MatchingServiceAssertion> getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public MatchingServiceIdaStatus getStatus() {
        return status;
    }
}
