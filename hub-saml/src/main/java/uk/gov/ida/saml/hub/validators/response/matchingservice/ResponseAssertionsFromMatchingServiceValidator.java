package uk.gov.ida.saml.hub.validators.response.matchingservice;

import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.validators.assertion.AssertionValidator;
import uk.gov.ida.saml.hub.exception.SamlValidationException;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.authnContextMissingError;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.missingAuthnStatement;

public class ResponseAssertionsFromMatchingServiceValidator {

    private AssertionValidator assertionValidator;
    private String hubEntityId;

    public ResponseAssertionsFromMatchingServiceValidator(AssertionValidator assertionValidator, String hubEntityId) {
        this.assertionValidator = assertionValidator;
        this.hubEntityId = hubEntityId;
    }

    public void validate(ValidatedResponse validatedResponse, ValidatedAssertions validatedAssertions) {
        if (!validatedResponse.isSuccess()) return;

        for (Assertion assertion : validatedAssertions.getAssertions()) {
            assertionValidator.validate(assertion, validatedResponse.getInResponseTo(), hubEntityId);

            if (assertion.getAuthnStatements().isEmpty()) {
                throw new SamlValidationException(missingAuthnStatement());
            }

            if (assertion.getAuthnStatements().get(0).getAuthnContext() == null) {
                throw new SamlValidationException(authnContextMissingError());
            }
        }
    }
}
