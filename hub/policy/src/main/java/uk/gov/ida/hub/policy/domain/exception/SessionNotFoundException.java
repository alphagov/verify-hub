package uk.gov.ida.hub.policy.domain.exception;

import uk.gov.ida.hub.policy.domain.SessionId;

import java.text.MessageFormat;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(SessionId sessionId) {
        super(MessageFormat.format("Session: {0} not found.", sessionId.getSessionId()));
    }

}
