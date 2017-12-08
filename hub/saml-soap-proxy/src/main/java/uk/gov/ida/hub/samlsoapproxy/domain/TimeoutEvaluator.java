package uk.gov.ida.hub.samlsoapproxy.domain;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import uk.gov.ida.hub.samlsoapproxy.exceptions.AttributeQueryTimeoutException;

import java.text.MessageFormat;

public class TimeoutEvaluator {

    public void hasAttributeQueryTimedOut(AttributeQueryContainerDto queryContainerDto) {
        DateTime timeout = queryContainerDto.getAttributeQueryClientTimeOut();
        DateTime timeOfCheck = DateTime.now();
        if(timeout.isBefore(timeOfCheck)){
            Duration duration = new Duration(timeout, timeOfCheck);
            throw new AttributeQueryTimeoutException(MessageFormat.format("Attribute Query timed out by {0} seconds.", duration.getStandardSeconds()));
        }
    }
}
