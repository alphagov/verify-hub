package uk.gov.ida.hub.samlsoapproxy.logging;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEvent;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.exceptions.ApplicationException;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.common.SessionId.NO_SESSION_CONTEXT_IN_ERROR;
import static uk.gov.ida.eventsink.EventDetailsKey.downstream_uri;
import static uk.gov.ida.eventsink.EventDetailsKey.message;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.EventTypes.ERROR_EVENT;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckEventLoggerTest {

    @Mock
    private EventSinkProxy eventSinkProxy;

    private ServiceInfoConfiguration serviceInfo = new ServiceInfoConfiguration("test");
    private HealthCheckEventLogger eventLogger;

    @Before
    public void setUp() {
        eventLogger = new HealthCheckEventLogger(eventSinkProxy, serviceInfo);
    }

    @Test
    public void shouldLogToEventSinkIfTheExceptionIsUnaudited() {
        URI uri = URI.create("uri-geller");
        ApplicationException unauditedException = ApplicationException.createUnauditedException(ExceptionType.INVALID_SAML, UUID.randomUUID(), uri);
        ImmutableMap<EventDetailsKey, String> details = ImmutableMap.of(
                downstream_uri, unauditedException.getUri().or(URI.create("uri-not-present")).toASCIIString(),
                message, unauditedException.getMessage());
        EventSinkHubEvent event = new EventSinkHubEvent(serviceInfo, NO_SESSION_CONTEXT_IN_ERROR, ERROR_EVENT,details);

        eventLogger.logException(unauditedException, "test error message");

        ArgumentCaptor<EventSinkHubEvent> captor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy, times(1)).logHubEvent(captor.capture());
        assertThat(event).isEqualToComparingOnlyGivenFields(captor.getValue(), "originatingService", "sessionId", "eventType", "details");
    }

    @Test
    public void shouldNotLogToEventSinkIfTheExceptionIsAudited() {
        ApplicationException unauditedException = ApplicationException.createAuditedException(ExceptionType.INVALID_SAML, UUID.randomUUID());

        eventLogger.logException(unauditedException, "test error message");

        verify(eventSinkProxy, times(0)).logHubEvent(any());
    }

    @Test
    public void shouldNotLogToEventSinkIfTheExceptionIsUnauditedButShouldNotBeaudited() {
        ApplicationException unauditedException = ApplicationException.createUnauditedException(ExceptionType.NETWORK_ERROR, UUID.randomUUID());

        eventLogger.logException(unauditedException, "test error message");

        verify(eventSinkProxy, times(0)).logHubEvent(any());
    }
}
