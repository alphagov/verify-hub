package uk.gov.ida.saml.hub.validators.response;

import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToIdpIdaStatusMappingsFactory;

public class EncryptedResponseFromIdpValidator extends ResponseFromIdpValidator {

    public EncryptedResponseFromIdpValidator(
            final SamlStatusToIdpIdaStatusMappingsFactory statusMappingsFactory) {
        super(statusMappingsFactory);
    }

    protected void validateAssertionPresence(Response response) {
        if (!response.getAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unencryptedAssertion();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        boolean responseWasSuccessful = response.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS);
        if (responseWasSuccessful && response.getEncryptedAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (!responseWasSuccessful && !response.getEncryptedAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.nonSuccessHasUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (responseWasSuccessful && response.getEncryptedAssertions().size() != 2) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unexpectedNumberOfAssertions(2, response.getEncryptedAssertions().size());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

    }
}
