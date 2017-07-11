package uk.gov.ida.saml.core.transformers.inbound.decorators;

import org.opensaml.saml.saml2.core.AuthnRequest;
import uk.gov.ida.saml.core.validators.DestinationValidator;

public class ValidateSamlAuthnRequestFromTransactionDestination {

    private final DestinationValidator validator;
    private final String expectedEndpoint;

    public ValidateSamlAuthnRequestFromTransactionDestination(
            DestinationValidator validator,
            String expectedEndpoint
    ) {
        this.validator = validator;
        this.expectedEndpoint = expectedEndpoint;
    }

    public void validate(AuthnRequest authnRequest) {
        validator.validate(authnRequest.getDestination(), expectedEndpoint);
    }
}
