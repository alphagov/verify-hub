package uk.gov.ida.hub.samlsoapproxy.exceptions;

public class AttributeQueryTimeoutException extends RuntimeException {
    public AttributeQueryTimeoutException() {
    }

    public AttributeQueryTimeoutException(String message) {
        super(message);
    }
}
