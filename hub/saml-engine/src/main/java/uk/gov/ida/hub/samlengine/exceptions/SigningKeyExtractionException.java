package uk.gov.ida.hub.samlengine.exceptions;

public class SigningKeyExtractionException extends RuntimeException {

    public SigningKeyExtractionException() {
    }

    public SigningKeyExtractionException(String message) {
        super(message);
    }

    public SigningKeyExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SigningKeyExtractionException(Throwable cause) {
        super(cause);
    }

    public SigningKeyExtractionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
