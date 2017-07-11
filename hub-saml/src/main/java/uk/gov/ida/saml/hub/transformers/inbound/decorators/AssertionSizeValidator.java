package uk.gov.ida.saml.hub.transformers.inbound.decorators;

import uk.gov.ida.saml.deserializers.validators.SizeValidator;

public class AssertionSizeValidator implements SizeValidator {

    @Override
    public void validate(String input) {
        // do nothing
    }
}
