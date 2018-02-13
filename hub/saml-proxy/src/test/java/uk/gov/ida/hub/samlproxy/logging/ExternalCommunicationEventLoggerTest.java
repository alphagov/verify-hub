package uk.gov.ida.hub.samlproxy.logging;

import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEvent;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.samlproxy.SamlProxyConfiguration;
import uk.gov.ida.shared.utils.IpAddressResolver;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.ida.common.ServiceInfoConfigurationBuilder.aServiceInfo;
import static uk.gov.ida.eventsink.EventDetailsKey.external_communication_type;
import static uk.gov.ida.eventsink.EventDetailsKey.external_endpoint;
import static uk.gov.ida.eventsink.EventDetailsKey.external_ip_address;
import static uk.gov.ida.eventsink.EventDetailsKey.message_id;
import static uk.gov.ida.eventsink.EventDetailsKey.principal_ip_address_as_seen_by_hub;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.EventTypes.EXTERNAL_COMMUNICATION_EVENT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.AUTHN_REQUEST;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.MATCHING_SERVICE_REQUEST;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.RESPONSE_FROM_HUB;

@RunWith(MockitoJUnitRunner.class)
public class ExternalCommunicationEventLoggerTest {
    private static final String SERVICE_NAME = "some-service-name";
    private static final ServiceInfoConfiguration SERVICE_INFO = aServiceInfo().withName(SERVICE_NAME).build();
    private static final SessionId SESSION_ID = SessionId.createNewSessionId();
    private static final String MESSAGE_ID = "some-message-id";
    private static final URI ENDPOINT_URL = URI.create("http://someurl.com");
    private static final String ENDPOINT_IP_ADDRESS = "1.2.3.4";
    private static final String PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB = "some-ip-address";

    @Mock
    private EventSinkProxy eventSinkProxy;

    @Mock
    private IpAddressResolver ipAddressResolver;

    @Mock
    private EventEmitter eventEmitter;

    @Mock
    private SamlProxyConfiguration configuration;

    private ExternalCommunicationEventLogger externalCommunicationEventLoggerWithoutRecordingSystem;
    private ExternalCommunicationEventLogger externalCommunicationEventLoggerWithRecordingSystem;

    @Before
    public void setUp() throws Exception {
        DateTimeFreezer.freezeTime();

        when(ipAddressResolver.lookupIpAddress(ENDPOINT_URL)).thenReturn(ENDPOINT_IP_ADDRESS);
        externalCommunicationEventLoggerWithoutRecordingSystem = new ExternalCommunicationEventLogger(SERVICE_INFO, eventSinkProxy, eventEmitter, ipAddressResolver, configuration);

        when(configuration.getSendToRecordingSystem()).thenReturn(true);
        externalCommunicationEventLoggerWithRecordingSystem = new ExternalCommunicationEventLogger(SERVICE_INFO, eventSinkProxy, eventEmitter, ipAddressResolver, configuration);

    }

    @After
    public void tearDown() throws Exception {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void logMatchingServiceRequest_shouldPassHubEventToEventSinkAndRecordingSystem() {
        externalCommunicationEventLoggerWithRecordingSystem.logMatchingServiceRequest(MESSAGE_ID, SESSION_ID, ENDPOINT_URL);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(external_communication_type, MATCHING_SERVICE_REQUEST);
        details.put(message_id, MESSAGE_ID);
        details.put(external_endpoint, ENDPOINT_URL.toString());
        details.put(external_ip_address, ENDPOINT_IP_ADDRESS);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            details
        );

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logMatchingServiceRequest_shouldPassHubEventToEventSinkOnly() {
        externalCommunicationEventLoggerWithoutRecordingSystem.logMatchingServiceRequest(MESSAGE_ID, SESSION_ID, ENDPOINT_URL);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(external_communication_type, MATCHING_SERVICE_REQUEST);
        details.put(message_id, MESSAGE_ID);
        details.put(external_endpoint, ENDPOINT_URL.toString());
        details.put(external_ip_address, ENDPOINT_IP_ADDRESS);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            details
        );

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verifyNoMoreInteractions(eventEmitter);
    }

    @Test
    public void logAuthenticationRequest_shouldPassHubEventToEventSinkAndRecordingSystem() {
        externalCommunicationEventLoggerWithRecordingSystem.logIdpAuthnRequest(MESSAGE_ID, SESSION_ID, ENDPOINT_URL, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(external_communication_type, AUTHN_REQUEST);
        details.put(message_id, MESSAGE_ID);
        details.put(external_endpoint, ENDPOINT_URL.toString());
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            details
        );

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logResponseFromHub_shouldPassHubEventToEventSinkAndRecordingSystem() {
        externalCommunicationEventLoggerWithRecordingSystem.logResponseFromHub(MESSAGE_ID, SESSION_ID, ENDPOINT_URL, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(external_communication_type, RESPONSE_FROM_HUB);
        details.put(message_id, MESSAGE_ID);
        details.put(external_endpoint, ENDPOINT_URL.toString());
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            details
        );

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    private class EventMatching extends ArgumentMatcher<EventSinkHubEvent> {

        private EventSinkHubEvent expectedEvent;

        private EventMatching(EventSinkHubEvent expectedEvent) {
            this.expectedEvent = expectedEvent;
        }

        @Override
        public boolean matches(Object other) {
            if (other == null || expectedEvent.getClass() != other.getClass()) {
                return false;
            }
            EventSinkHubEvent actualEvent = (EventSinkHubEvent) other;
            return
                Objects.equals(expectedEvent.getTimestamp(), actualEvent.getTimestamp()) &&
                    Objects.equals(expectedEvent.getOriginatingService(), actualEvent.getOriginatingService()) &&
                    Objects.equals(expectedEvent.getSessionId(), actualEvent.getSessionId()) &&
                    Objects.equals(expectedEvent.getEventType(), actualEvent.getEventType()) &&
                    Objects.equals(expectedEvent.getDetails(), actualEvent.getDetails());
        }
    }
}
