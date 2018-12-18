package uk.gov.ida.hub.samlsoapproxy.exceptions;

public class EncryptionKeyExtractionException extends RuntimeException {
    public EncryptionKeyExtractionException(String message, Exception causee) {
        super(message, causee);
    }

    public EncryptionKeyExtractionException(String message) {
        super(message);
    }

    public static EncryptionKeyExtractionException noKeyFound() {
        return new EncryptionKeyExtractionException("did not find an encryption key in metadata");
    }
}
