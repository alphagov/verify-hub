package uk.gov.ida.hub.policy.exception;

import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import static java.text.MessageFormat.format;

public class InvalidSessionStateException extends RuntimeException {

    private final Class<? extends State> expectedState;
    private final Class<? extends State> actualState;
    private final SessionId sessionId;

    public InvalidSessionStateException(SessionId sessionId, Class<? extends State> expectedState, Class<? extends State> actualState) {
        super(format("Session ID: {3}{2}Expected State: {0}{2}Session State: {1}",
                expectedState.getSimpleName(),
                actualState.getSimpleName(),
                System.getProperty("line.separator"),
                sessionId.getSessionId()));
        this.sessionId = sessionId;
        this.expectedState = expectedState;
        this.actualState = actualState;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public Class<? extends State> getExpectedState() {
        return expectedState;
    }

    public Class<? extends State> getActualState() {
        return actualState;
    }
}
