package uk.gov.ida.hub.samlsoapproxy.logging;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventemitter.EventDetailsKey;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEvent;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;
import uk.gov.ida.exceptions.ApplicationException;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.common.SessionId.NO_SESSION_CONTEXT_IN_ERROR;
import static uk.gov.ida.eventemitter.EventDetailsKey.downstream_uri;
import static uk.gov.ida.eventemitter.EventDetailsKey.message;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.ERROR_EVENT;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckEventLoggerTest {

    @Mock
    private EventSinkProxy eventSinkProxy;

    @Mock
    private EventEmitter eventEmitter;

    private ServiceInfoConfiguration serviceInfo = new ServiceInfoConfiguration("test");
    private HealthCheckEventLogger eventLogger;

    @Before
    public void setUp() {
        eventLogger = new HealthCheckEventLogger(eventSinkProxy, eventEmitter, serviceInfo);
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

        ArgumentCaptor<EventSinkHubEvent> eventSinkCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        ArgumentCaptor<EventSinkHubEvent> eventEmitterCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        verify(eventSinkProxy, times(1)).logHubEvent(eventSinkCaptor.capture());
        verify(eventEmitter, times(1)).record(eventEmitterCaptor.capture());


        assertThat(event).isEqualToComparingOnlyGivenFields(eventSinkCaptor.getValue(), "originatingService", "sessionId", "eventType", "details");
        assertThat(event).isEqualToComparingOnlyGivenFields(eventEmitterCaptor.getValue(), "originatingService", "sessionId", "eventType", "details");
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
