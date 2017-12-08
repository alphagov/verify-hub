package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.SessionId;

import java.util.UUID;

public class SessionIdBuilder {
    private String sessionId = UUID.randomUUID().toString();

    public static SessionIdBuilder aSessionId() {
        return new SessionIdBuilder();
    }

    public SessionId build() {
        return new SessionId(sessionId);
    }

    public SessionIdBuilder with(final String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
}
