package uk.gov.ida.hub.policy.domain.exception;

import org.slf4j.event.Level;
import uk.gov.ida.common.ExceptionType;

public class SessionCreationFailureException extends RuntimeException {

    private final Level logLevel;

    private final ExceptionType exceptionType;

    public SessionCreationFailureException(String errorMessage, Level logLevel, ExceptionType exceptionType) {
        this(errorMessage, null, logLevel, exceptionType);
    }

    public SessionCreationFailureException(
        String errorMessage,
        Exception cause,
        Level logLevel,
        ExceptionType exceptionType
    ) {
        super(errorMessage, cause);
        this.logLevel = logLevel;
        this.exceptionType = exceptionType;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public ExceptionType getExceptionType() {
        return exceptionType;
    }

    public static SessionCreationFailureException assertionConsumerServiceUrlNotMatching(
        String assertionConsumerUrl,
        String configFileUrl,
        String issuer
    ) {
        String errorMessage = String.format(
            "AssertionConsumerUrl (%s) doesn't match consumer url in config file (%s). Issuer was: %s",
            assertionConsumerUrl,
            configFileUrl,
            issuer
        );

        return new SessionCreationFailureException(
            errorMessage,
            Level.ERROR,
            ExceptionType.INVALID_ASSERTION_CONSUMER_URL
        );
    }

    public static SessionCreationFailureException configServiceException(Exception e) {
        return new SessionCreationFailureException(e.getMessage(), e, Level.ERROR, ExceptionType.INVALID_ASSERTION_CONSUMER_INDEX);
    }
}
