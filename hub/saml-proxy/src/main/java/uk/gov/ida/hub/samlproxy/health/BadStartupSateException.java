package uk.gov.ida.hub.samlproxy.health;

class BadStartupSateException extends RuntimeException {
    public BadStartupSateException(String message, Throwable error) {
        super(message, error);
    }
}
