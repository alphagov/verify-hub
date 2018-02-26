package uk.gov.ida.saml.hub.validators.response.matchingservice;

import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.core.validators.assertion.AssertionValidator;
import uk.gov.ida.saml.hub.validators.response.ResponseAssertionsValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

public class ResponseAssertionsFromMatchingServiceValidator extends ResponseAssertionsValidator {

    public ResponseAssertionsFromMatchingServiceValidator(
            AssertionValidator assertionValidator,
            String hubEntityId) {

        super(assertionValidator, hubEntityId);
    }

    @Override
    public void validate(ValidatedResponse validatedResponse, ValidatedAssertions validatedAssertions) {
        if (validatedResponse.isSuccess()) {
            super.validate(validatedResponse, validatedAssertions);
            for (Assertion assertion : validatedAssertions.getAssertions()) {
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
}
