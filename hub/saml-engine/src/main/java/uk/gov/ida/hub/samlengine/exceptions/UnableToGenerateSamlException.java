package uk.gov.ida.hub.samlengine.exceptions;

import org.slf4j.event.Level;

public class UnableToGenerateSamlException extends RuntimeException {
    private final Level logLevel;

    public UnableToGenerateSamlException(String message, Exception cause, Level logLevel) {
        super(message, cause);
        this.logLevel = logLevel;
    }

    public Level getLogLevel() {
        return logLevel;
    }
}
