package uk.gov.ida.saml.hub.validators.response.common;

import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.core.validators.DestinationValidator;

public class ResponseDestinationValidator {

    private final DestinationValidator validator;
    private final String expectedEndpoint;

    public ResponseDestinationValidator(
            DestinationValidator validator,
            String expectedEndpoint
    ) {
        this.validator = validator;
        this.expectedEndpoint = expectedEndpoint;
    }

    public void validate(Response response) {
        validator.validate(response.getDestination(), expectedEndpoint);
    }
}
