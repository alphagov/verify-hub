package uk.gov.ida.saml.hub.domain;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.IdaMatchingServiceResponse;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;

public class InboundResponseFromMatchingService extends IdaMatchingServiceResponse {
    private Optional<PassthroughAssertion> matchingServiceAssertion;
    private MatchingServiceIdaStatus status;

    @SuppressWarnings("unused") // needed for JAXB
    private InboundResponseFromMatchingService() {
    }

    public InboundResponseFromMatchingService(String responseId, String inResponseTo, String issuer, DateTime issueInstant, MatchingServiceIdaStatus status, Optional<PassthroughAssertion> matchingServiceAssertion) {
        super(responseId, inResponseTo, issuer, issueInstant);
        this.matchingServiceAssertion = matchingServiceAssertion;
        this.status = status;
    }

    public Optional<PassthroughAssertion> getMatchingServiceAssertion() {
        return matchingServiceAssertion;
    }

    public MatchingServiceIdaStatus getStatus() {
        return status;
    }
}
