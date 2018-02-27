package uk.gov.ida.saml.hub.transformers.inbound.providers;

import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import uk.gov.ida.saml.hub.domain.InboundHealthCheckResponseFromMatchingService;
import uk.gov.ida.saml.hub.transformers.inbound.InboundHealthCheckResponseFromMatchingServiceUnmarshaller;
import uk.gov.ida.saml.hub.validators.response.matchingservice.HealthCheckResponseFromMatchingServiceValidator;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

public class DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer {

    private final InboundHealthCheckResponseFromMatchingServiceUnmarshaller healthCheckUnmarshaller;
    private final SamlResponseSignatureValidator samlResponseSignatureValidator;
    private final HealthCheckResponseFromMatchingServiceValidator healthCheckResponseFromMatchingServiceValidator;

    public DecoratedSamlResponseToInboundHealthCheckResponseFromMatchingServiceTransformer(
            InboundHealthCheckResponseFromMatchingServiceUnmarshaller healthCheckUnmarshaller,
            SamlResponseSignatureValidator samlResponseSignatureValidator,
            HealthCheckResponseFromMatchingServiceValidator healthCheckResponseFromMatchingServiceValidator) {

        this.healthCheckUnmarshaller = healthCheckUnmarshaller;
        this.samlResponseSignatureValidator = samlResponseSignatureValidator;
        this.healthCheckResponseFromMatchingServiceValidator = healthCheckResponseFromMatchingServiceValidator;
    }

    public InboundHealthCheckResponseFromMatchingService transform(Response response) {
        healthCheckResponseFromMatchingServiceValidator.validate(response);
        samlResponseSignatureValidator.validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
        return healthCheckUnmarshaller.fromSaml(response);
    }
}
