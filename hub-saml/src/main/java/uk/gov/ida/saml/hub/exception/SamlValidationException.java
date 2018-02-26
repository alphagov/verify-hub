package uk.gov.ida.saml.hub.exception;

import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

public class SamlValidationException extends SamlTransformationErrorException {
    public SamlValidationException(SamlValidationSpecificationFailure failure) {
        super(failure.getErrorMessage(), failure.getLogLevel());
    }
}
