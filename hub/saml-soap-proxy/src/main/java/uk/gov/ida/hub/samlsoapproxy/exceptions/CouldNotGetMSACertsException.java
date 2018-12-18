package uk.gov.ida.hub.samlsoapproxy.exceptions;

public class CouldNotGetMSACertsException extends RuntimeException {
    public CouldNotGetMSACertsException(Exception e) {
        super(e);
    }
}
