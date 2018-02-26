package uk.gov.ida.saml.hub.validators.response.matchingservice;

import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.validators.assertion.AssertionValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

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
            if (assertion.getAuthnStatements().size() == 0) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingAuthnStatement();
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
            if (assertion.getAuthnStatements().get(0).getAuthnContext() == null) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.authnContextMissingError();
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
        }
    }
}
