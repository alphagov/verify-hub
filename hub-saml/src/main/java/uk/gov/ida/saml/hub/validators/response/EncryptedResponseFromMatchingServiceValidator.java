package uk.gov.ida.saml.hub.validators.response;

import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
public class EncryptedResponseFromMatchingServiceValidator extends ResponseFromMatchingServiceValidator {

    protected void validateAssertionPresence(Response response) {
        if (!response.getAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unencryptedAssertion();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        boolean responseWasSuccessful = response.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS);
        boolean responseHasNoAssertions = response.getEncryptedAssertions().isEmpty();

        if (responseWasSuccessful && responseHasNoAssertions) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (!responseWasSuccessful && !responseHasNoAssertions) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.nonSuccessHasUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (response.getEncryptedAssertions().size() > 1) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unexpectedNumberOfAssertions(1, response.getEncryptedAssertions().size());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
