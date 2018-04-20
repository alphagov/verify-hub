package uk.gov.ida.saml.hub.transformers.inbound;

import com.google.common.base.Optional;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.hub.domain.InboundResponseFromMatchingService;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

public class InboundResponseFromMatchingServiceUnmarshaller {
    private PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller;
    private MatchingServiceIdaStatusUnmarshaller statusUnmarshaller;

    public InboundResponseFromMatchingServiceUnmarshaller(
            PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller,
            MatchingServiceIdaStatusUnmarshaller statusUnmarshaller) {
        this.passthroughAssertionUnmarshaller = passthroughAssertionUnmarshaller;
        this.statusUnmarshaller = statusUnmarshaller;
    }

    public InboundResponseFromMatchingService fromSaml(ValidatedResponse validatedResponse, ValidatedAssertions validatedAssertions) {
        Optional<PassthroughAssertion> idaAssertion = null;
        if (validatedAssertions.getAssertions().size() > 0){
            idaAssertion = Optional.fromNullable(passthroughAssertionUnmarshaller.fromAssertion(validatedAssertions.getAssertions().get(0)));
        }

        MatchingServiceIdaStatus transformedStatus = statusUnmarshaller.fromSaml(validatedResponse.getStatus());

        return new InboundResponseFromMatchingService(
                validatedResponse.getID(),
                validatedResponse.getInResponseTo(),
                validatedResponse.getIssuer().getValue(),
                validatedResponse.getIssueInstant(),
                transformedStatus,
                idaAssertion);
    }
}
