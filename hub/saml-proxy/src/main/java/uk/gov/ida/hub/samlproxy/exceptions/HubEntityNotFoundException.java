package uk.gov.ida.hub.samlproxy.exceptions;

public class HubEntityNotFoundException extends RuntimeException {
    public HubEntityNotFoundException(String message) {
        super(message);
    }
}
