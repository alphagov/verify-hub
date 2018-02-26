package uk.gov.ida.saml.hub.validators.response.idp;

import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.validators.assertion.AuthnStatementAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.IPAddressValidator;
import uk.gov.ida.saml.core.validators.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.MatchingDatasetAssertionValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

public class ResponseAssertionsFromIdpValidator {

    private final IdentityProviderAssertionValidator identityProviderAssertionValidator;
    private final MatchingDatasetAssertionValidator matchingDatasetAssertionValidator;
    private final AuthnStatementAssertionValidator authnStatementAssertionValidator;
    private final IPAddressValidator ipAddressValidator;
    private String hubEntityId;

    public ResponseAssertionsFromIdpValidator(
            IdentityProviderAssertionValidator assertionValidator,
            MatchingDatasetAssertionValidator matchingDatasetAssertionValidator,
            AuthnStatementAssertionValidator authnStatementAssertionValidator,
            IPAddressValidator ipAddressValidator,
            String hubEntityId) {
        this.identityProviderAssertionValidator = assertionValidator;
        this.matchingDatasetAssertionValidator = matchingDatasetAssertionValidator;
        this.authnStatementAssertionValidator = authnStatementAssertionValidator;
        this.ipAddressValidator = ipAddressValidator;
        this.hubEntityId = hubEntityId;
    }

    public void validate(ValidatedResponse validatedResponse, ValidatedAssertions validatedAssertions) {
        validatedAssertions.getAssertions().forEach(
                assertion -> identityProviderAssertionValidator.validate(assertion, validatedResponse.getInResponseTo(), hubEntityId)
        );

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
