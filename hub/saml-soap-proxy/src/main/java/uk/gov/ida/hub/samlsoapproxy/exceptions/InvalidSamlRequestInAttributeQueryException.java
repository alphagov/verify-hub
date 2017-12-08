package uk.gov.ida.hub.samlsoapproxy.exceptions;

public class InvalidSamlRequestInAttributeQueryException extends RuntimeException {
    public InvalidSamlRequestInAttributeQueryException(String message, Exception cause) {
        super(message, cause);
    }
}
