package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.IdaMatchingServiceResponse;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;

public class InboundHealthCheckResponseFromMatchingService extends IdaMatchingServiceResponse {
    private MatchingServiceIdaStatus status;

    @SuppressWarnings("unused") // needed for JAXB
    private InboundHealthCheckResponseFromMatchingService() {
    }

    public InboundHealthCheckResponseFromMatchingService(
            final String responseId,
            final String inResponseTo,
            final String issuer,
            final DateTime issueInstant,
            final MatchingServiceIdaStatus status) {

        super(responseId, inResponseTo, issuer, issueInstant);

        this.status = status;
    }

    public MatchingServiceIdaStatus getStatus() {
        return status;
    }
}
