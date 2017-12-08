package uk.gov.ida.hub.policy.matchers;

import org.assertj.core.api.Condition;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;

public class IsEventType extends Condition<EventSinkHubEvent> {

    private String expectedEventType;

    public IsEventType(String expectedEventType) {
        this.expectedEventType = expectedEventType;
    }

    public static IsEventType isEventType(String expectedEventType){
        return new IsEventType(expectedEventType);
    }

    @Override
    public boolean matches(EventSinkHubEvent value) {
        return expectedEventType.equals(value.getEventType());
    }
}
