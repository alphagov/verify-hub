package uk.gov.ida.hub.samlengine.exceptions;

public class EncryptionKeyExtractionException extends RuntimeException {
    public EncryptionKeyExtractionException(String message, Exception cause) {
        super(message, cause);
    }

    public EncryptionKeyExtractionException(String message) {
        super(message);
    }

    public static EncryptionKeyExtractionException noKeyFound() {
        return new EncryptionKeyExtractionException("did not find an encryption key in metadata");
    }
}
