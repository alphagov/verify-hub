package uk.gov.ida.saml.hub.validators.response.idp;

import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

public class IdpResponseValidatorResultContainer {
    private final ValidatedResponse validatedResponse;
    private final ValidatedAssertions validatedAssertions;

    IdpResponseValidatorResultContainer(
            ValidatedResponse validatedResponse,
            ValidatedAssertions validatedAssertions) {
            this.validatedResponse = validatedResponse;
            this.validatedAssertions = validatedAssertions;
    }

    public ValidatedResponse getValidatedResponse() {
        return this.validatedResponse;
    }

    public ValidatedAssertions getValidatedAssertions() {
        return this.validatedAssertions;
    }
}
