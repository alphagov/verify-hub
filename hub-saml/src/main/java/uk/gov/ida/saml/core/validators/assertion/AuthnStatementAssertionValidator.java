package uk.gov.ida.saml.core.validators.assertion;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
public class AuthnStatementAssertionValidator {

    private final DuplicateAssertionValidator duplicateAssertionValidator;

    public AuthnStatementAssertionValidator(DuplicateAssertionValidator duplicateAssertionValidator) {
        this.duplicateAssertionValidator = duplicateAssertionValidator;
    }

    public void validate(Assertion assertion) {
        validateAuthnStatement(assertion.getAuthnStatements().get(0));

        if(!duplicateAssertionValidator.valid(assertion)){
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.authnStatementAlreadyReceived(assertion.getID());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

    private void validateAuthnStatement(AuthnStatement authnStatement) {
        if (authnStatement.getAuthnContext() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.authnContextMissingError();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (authnStatement.getAuthnContext().getAuthnContextClassRef() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.authnContextClassRefMissing();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        if (authnStatement.getAuthnContext().getAuthnContextClassRef().getAuthnContextClassRef() == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.authnContextClassRefValueMissing();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
