package uk.gov.ida.saml.hub.validators.response.common;

import javax.inject.Inject;
import uk.gov.ida.saml.hub.validators.StringSizeValidator;

public class ResponseMaxSizeValidator extends ResponseSizeValidator {
    private static final int LOWER_BOUND = 0;

    @Inject
    public ResponseMaxSizeValidator(StringSizeValidator validator) {
        super(validator);
    }

    @Override
    protected int getLowerBound() {
        return LOWER_BOUND;
    }

}
