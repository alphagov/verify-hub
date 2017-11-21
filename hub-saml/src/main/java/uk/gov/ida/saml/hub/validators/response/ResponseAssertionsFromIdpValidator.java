package uk.gov.ida.saml.hub.validators.response;

import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.core.validators.assertion.AuthnStatementAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.IPAddressValidator;
import uk.gov.ida.saml.core.validators.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.MatchingDatasetAssertionValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

public class ResponseAssertionsFromIdpValidator extends ResponseAssertionsValidator {

    private final IdentityProviderAssertionValidator identityProviderAssertionValidator;
    private final MatchingDatasetAssertionValidator matchingDatasetAssertionValidator;
    private final AuthnStatementAssertionValidator authnStatementAssertionValidator;
    private final IPAddressValidator ipAddressValidator;

    public ResponseAssertionsFromIdpValidator(
            IdentityProviderAssertionValidator assertionValidator,
            MatchingDatasetAssertionValidator matchingDatasetAssertionValidator,
            AuthnStatementAssertionValidator authnStatementAssertionValidator,
            IPAddressValidator ipAddressValidator,
            String hubEntityId) {

        super(assertionValidator, hubEntityId);

        this.identityProviderAssertionValidator = assertionValidator;
        this.matchingDatasetAssertionValidator = matchingDatasetAssertionValidator;
        this.authnStatementAssertionValidator = authnStatementAssertionValidator;
        this.ipAddressValidator = ipAddressValidator;
    }

    @Override
    public void validate(ValidatedResponse validatedResponse, ValidatedAssertions validatedAssertions) {
        super.validate(validatedResponse, validatedAssertions);

        if (validatedResponse.isSuccess()) {

            Assertion matchingDatasetAssertion = getMatchingDatasetAssertion(validatedAssertions);
            Assertion authnStatementAssertion = getAuthnStatementAssertion(validatedAssertions);

            if (authnStatementAssertion.getAuthnStatements().size() > 1) {
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.multipleAuthnStatements();
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }

            matchingDatasetAssertionValidator.validate(matchingDatasetAssertion, validatedResponse.getIssuer().getValue());
            authnStatementAssertionValidator.validate(authnStatementAssertion);
            identityProviderAssertionValidator.validateConsistency(authnStatementAssertion, matchingDatasetAssertion);
            ipAddressValidator.validate(authnStatementAssertion);
        }
    }

    private Assertion getAuthnStatementAssertion(ValidatedAssertions validatedAssertions) {
        return validatedAssertions.getAuthnStatementAssertion().orElseThrow(() -> {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingAuthnStatement();
            return new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        });
    }

    private Assertion getMatchingDatasetAssertion(ValidatedAssertions validatedAssertions) {
        return validatedAssertions.getMatchingDatasetAssertion().orElseThrow(() -> {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingMatchingMds();
            return new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        });
    }
}
