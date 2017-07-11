package uk.gov.ida.saml.hub.validators.response;

import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.validators.assertion.AssertionValidator;
import uk.gov.ida.saml.hub.HubConstants;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

public class ResponseAssertionsValidator {

    private final AssertionValidator assertionValidator;
    private final String hubEntityId;

    public ResponseAssertionsValidator(
            AssertionValidator assertionValidator,
            String hubEntityId) {

        this.assertionValidator = assertionValidator;
        this.hubEntityId = hubEntityId;
    }

    public void validate(ValidatedResponse validatedResponse, ValidatedAssertions validatedAssertions) {
        for (Assertion assertion : validatedAssertions.getAssertions()) {
            assertionValidator.validate(assertion, validatedResponse.getInResponseTo(), hubEntityId);
        }
    }
}
