package uk.gov.ida.hub.policy.domain;

import java.io.Serializable;
import java.util.UUID;

public class SessionId implements Serializable {

    public static final SessionId SESSION_ID_DOES_NOT_EXIST_YET = new SessionId("SESSION-DOES-NOT-EXIST-YET");
    public static final SessionId NO_SESSION_CONTEXT_IN_ERROR = new SessionId("NO-SESSION-CONTEXT-IN-ERROR");

    private String sessionId;

    public static SessionId createNewSessionId() {
        return new SessionId(UUID.randomUUID().toString());
    }

    @SuppressWarnings("unused")//Needed by JAXB
    private SessionId() {
    }

    public SessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    // Fix: This should go, but JsonClient depends on it
    @Override
    public String toString() {
        return sessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SessionId otherSessionId = (SessionId) o;

        return sessionId == null ? otherSessionId.sessionId == null : sessionId.equals(otherSessionId.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId != null ? sessionId.hashCode() : 0;
    }
}
