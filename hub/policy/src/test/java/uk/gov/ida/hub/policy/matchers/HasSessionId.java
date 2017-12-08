package uk.gov.ida.hub.policy.matchers;

import org.assertj.core.api.Condition;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.SessionId;

public class HasSessionId extends Condition<EventSinkHubEvent> {
    private String sessionId;

    public HasSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public static HasSessionId hasSessionId(SessionId sessionId){
        return new HasSessionId(sessionId.getSessionId());
    }

    @Override
    public boolean matches(EventSinkHubEvent value) {
        return   (value.getSessionId().equals(sessionId));
    }
}
