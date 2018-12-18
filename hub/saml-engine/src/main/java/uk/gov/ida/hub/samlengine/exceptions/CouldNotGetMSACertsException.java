package uk.gov.ida.hub.samlengine.exceptions;

public class CouldNotGetMSACertsException extends RuntimeException {
    public CouldNotGetMSACertsException(Exception e) {
        super(e);
    }
}
