package uk.gov.ida.hub.policy.matchers;

import org.assertj.core.api.Condition;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;

import java.util.Map;

public class HasDetail extends Condition<EventSinkHubEvent> {

    private final EventDetailsKey eventDetailsKey;
    private String expectedDetail;

    public HasDetail(EventDetailsKey eventDetailsKey, String expectedDetail) {
        this.eventDetailsKey = eventDetailsKey;
        this.expectedDetail = expectedDetail;
    }

    public static HasDetail hasDetail(EventDetailsKey eventDetailsKey, String expectedDetail){
        return new HasDetail(eventDetailsKey, expectedDetail);
    }

    @Override
    public boolean matches(EventSinkHubEvent value) {
        Map<EventDetailsKey,String> details = value.getDetails();
        return details.containsKey(eventDetailsKey) && details.get(eventDetailsKey).equals(expectedDetail);
    }
}
