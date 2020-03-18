package uk.gov.ida.hub.config.exceptions;

public class NoCertificateFoundException extends RuntimeException {
    public NoCertificateFoundException(){}
    public NoCertificateFoundException(String message) { super(message); }
}
