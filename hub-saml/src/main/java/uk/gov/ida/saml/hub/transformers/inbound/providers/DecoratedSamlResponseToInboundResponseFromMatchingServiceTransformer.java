package uk.gov.ida.saml.hub.transformers.inbound.providers;

import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.hub.domain.InboundResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.InboundResponseFromMatchingServiceUnmarshaller;
import uk.gov.ida.saml.hub.validators.response.matchingservice.MatchingServiceResponseValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

public class DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer {

    private final InboundResponseFromMatchingServiceUnmarshaller responseUnmarshaller;
    private final MatchingServiceResponseValidator matchingServiceResponseValidator;

    public DecoratedSamlResponseToInboundResponseFromMatchingServiceTransformer(
        MatchingServiceResponseValidator matchingServiceResponseValidator,
        InboundResponseFromMatchingServiceUnmarshaller responseUnmarshaller
        ) {
        this.responseUnmarshaller = responseUnmarshaller;
        this.matchingServiceResponseValidator = matchingServiceResponseValidator;
    }

    public InboundResponseFromMatchingService transform(Response response) {
        matchingServiceResponseValidator.validate(response);
        ValidatedResponse validatedResponse = matchingServiceResponseValidator.getValidatedResponse();
        ValidatedAssertions validatedAssertions = matchingServiceResponseValidator.getValidatedAssertions();
        return responseUnmarshaller.fromSaml(validatedResponse, validatedAssertions);
    }
}
