package uk.gov.ida.hub.samlengine.validation.country;

import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.hub.domain.CountryAuthenticationStatus;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToCountryAuthenticationStatusCodeMapper;
import uk.gov.ida.saml.hub.validators.response.idp.components.EncryptedResponseFromIdpValidator;

public class ResponseFromCountryValidator extends EncryptedResponseFromIdpValidator<CountryAuthenticationStatus.Status> {

    public ResponseFromCountryValidator(SamlStatusToCountryAuthenticationStatusCodeMapper samlStatusToCountryAuthenticationStatusCodeMapper) {
        super(samlStatusToCountryAuthenticationStatusCodeMapper);
    }

    protected void validateAssertionPresence(Response response) {
        boolean responseWasSuccessful = response.getStatus().getStatusCode().getValue().equals(StatusCode.SUCCESS);

        if (responseWasSuccessful && !response.getAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unencryptedAssertion();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (responseWasSuccessful && response.getEncryptedAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingSuccessUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (!responseWasSuccessful && !response.getEncryptedAssertions().isEmpty()) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.nonSuccessHasUnEncryptedAssertions();
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (responseWasSuccessful && response.getEncryptedAssertions().size() != 1) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unexpectedNumberOfAssertions(1, response.getEncryptedAssertions().size());
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
