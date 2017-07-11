package uk.gov.ida.saml.hub.exception;

import org.apache.log4j.Level;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;

public class SamlDuplicateRequestIdException extends SamlTransformationErrorException {
    public SamlDuplicateRequestIdException(String errorMessage, Exception cause, Level logLevel) {
        super(errorMessage, cause, logLevel);
    }

    public SamlDuplicateRequestIdException(String errorMessage, Level logLevel) {
        super(errorMessage, logLevel);
    }
}
