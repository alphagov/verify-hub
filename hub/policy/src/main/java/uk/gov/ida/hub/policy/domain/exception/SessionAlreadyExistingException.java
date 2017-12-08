package uk.gov.ida.hub.policy.domain.exception;

import uk.gov.ida.hub.policy.domain.SessionId;

public class SessionAlreadyExistingException extends RuntimeException {

    private final SessionId sessionId;

    public SessionAlreadyExistingException(String message, SessionId sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }
}
