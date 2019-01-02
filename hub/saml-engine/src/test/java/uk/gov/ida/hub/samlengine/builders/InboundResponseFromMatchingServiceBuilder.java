package uk.gov.ida.hub.samlengine.builders;

import java.util.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.hub.domain.InboundResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.MatchingServiceIdaStatus;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class InboundResponseFromMatchingServiceBuilder extends ResponseBuilder<InboundResponseFromMatchingServiceBuilder> {

    private Optional<PassthroughAssertion> matchingServiceAssertion = empty();

    public static InboundResponseFromMatchingServiceBuilder anInboundResponseFromMatchingService() {
        return new InboundResponseFromMatchingServiceBuilder()
                .withResponseId("response-id")
                .withInResponseTo("request-id")
                .withIssuerId("issuer-id")
                .withIssueInstant(DateTime.now())
                .withStatus(MatchingServiceIdaStatus.MatchingServiceMatch);
    }

    public InboundResponseFromMatchingService build() {
        return new InboundResponseFromMatchingService(
                responseId,
                inResponseTo,
                issuerId,
                issueInstant,
                status,
                matchingServiceAssertion);
    }

    public InboundResponseFromMatchingServiceBuilder withMatchingServiceAssertion(PassthroughAssertion assertion) {
        this.matchingServiceAssertion = ofNullable(assertion);
        return this;
    }
}
