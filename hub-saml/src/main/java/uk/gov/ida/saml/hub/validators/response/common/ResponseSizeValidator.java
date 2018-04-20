package uk.gov.ida.saml.hub.validators.response.common;

import com.google.inject.Inject;
import uk.gov.ida.saml.deserializers.validators.SizeValidator;
import uk.gov.ida.saml.hub.validators.StringSizeValidator;


public class ResponseSizeValidator implements SizeValidator {
    // Ensures someone doing nasty things cannot get loads of data out of core hub in a single response

    private static final int LOWER_BOUND = 1400;
    private static final int UPPER_BOUND = 50000;

    private final StringSizeValidator validator;

    @Inject
    public ResponseSizeValidator(StringSizeValidator validator) {
        this.validator = validator;
    }

    @Override
    public void validate(String input) {
        validator.validate(input, getLowerBound(), getUpperBound());
    }

    private int getUpperBound() {
        return UPPER_BOUND;
    }

    protected int getLowerBound() {
        return LOWER_BOUND;
    }
}
