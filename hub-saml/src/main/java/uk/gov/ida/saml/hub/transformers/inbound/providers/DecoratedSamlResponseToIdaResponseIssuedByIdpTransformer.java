package uk.gov.ida.saml.hub.transformers.inbound.providers;

import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;
import uk.gov.ida.saml.hub.transformers.inbound.IdaResponseFromIdpUnmarshaller;
import uk.gov.ida.saml.hub.validators.response.idp.IdpResponseValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;


import java.util.function.Function;

public class DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer implements Function<Response, InboundResponseFromIdp> {

    private final IdaResponseFromIdpUnmarshaller idaResponseUnmarshaller;
    private final IdpResponseValidator idpResponseValidator;
    

    public DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(
            IdpResponseValidator idpResponseValidator,
            IdaResponseFromIdpUnmarshaller idaResponseUnmarshaller) {
        this.idaResponseUnmarshaller = idaResponseUnmarshaller;
        this.idpResponseValidator = idpResponseValidator;
    }

    @Override
    public InboundResponseFromIdp apply(Response response) {
        this.idpResponseValidator.validate(response);
        ValidatedResponse validatedResponse = this.idpResponseValidator.getValidatedResponse();
        ValidatedAssertions validatedAssertions = this.idpResponseValidator.getValidatedAssertions();
        return idaResponseUnmarshaller.fromSaml(validatedResponse, validatedAssertions);
    }
}
