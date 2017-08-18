package uk.gov.ida.saml.hub.exception;

import org.slf4j.event.Level;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;

public class SamlRequestTooOldException extends SamlTransformationErrorException {
    public SamlRequestTooOldException(String errorMessage, Exception cause, Level logLevel) {
        super(errorMessage, cause, logLevel);
    }

    public SamlRequestTooOldException(String errorMessage, Level logLevel) {
        super(errorMessage, logLevel);
    }
}
