package uk.gov.ida.hub.samlengine.exceptions;

import org.slf4j.event.Level;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.security.exception.SamlFailedToDecryptException;

public class SamlContextException extends RuntimeException {
    private final String messageId;
    private final String entityId;
    private final Level logLevel;

    public SamlContextException(String messageId, String entityId, SamlTransformationErrorException cause) {
        super(cause);
        this.messageId = messageId;
        this.entityId = entityId;
        this.logLevel = cause.getLogLevel();
    }

    @Override
    public String getMessage() {
        return "Error while processing message from " + entityId + " with ID " + messageId + ": " + getCause().getMessage();
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public ExceptionType getExceptionType() {
        if (getCause() instanceof SamlFailedToDecryptException) {
            return ExceptionType.INVALID_SAML_FAILED_TO_DECRYPT;
        } else {
            return ExceptionType.INVALID_SAML;
        }
    }
}
