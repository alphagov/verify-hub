package uk.gov.ida.hub.policy.exception;

import uk.gov.ida.hub.policy.domain.SessionId;

import static java.text.MessageFormat.format;

public class EidasNotSupportedException extends RuntimeException {
    private final SessionId sessionId;

    public EidasNotSupportedException(SessionId sessionId) {
        super(format("EIDAS is not supported. Session Id: {0}",
                sessionId.getSessionId()));
        this.sessionId = sessionId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }
}
