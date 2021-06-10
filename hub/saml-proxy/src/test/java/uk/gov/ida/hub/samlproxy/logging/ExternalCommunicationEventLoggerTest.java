package uk.gov.ida.hub.samlproxy.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEvent;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;
import uk.gov.ida.shared.utils.IpAddressResolver;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.common.ServiceInfoConfigurationBuilder.aServiceInfo;
import static uk.gov.ida.eventemitter.EventDetailsKey.external_communication_type;
import static uk.gov.ida.eventemitter.EventDetailsKey.external_endpoint;
import static uk.gov.ida.eventemitter.EventDetailsKey.external_ip_address;
import static uk.gov.ida.eventemitter.EventDetailsKey.message_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.principal_ip_address_as_seen_by_hub;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.EXTERNAL_COMMUNICATION_EVENT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.AUTHN_REQUEST;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.MATCHING_SERVICE_REQUEST;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.RESPONSE_FROM_HUB;

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

    private ExternalCommunicationEventLogger externalCommunicationEventLogger;

    @Before
    public void setUp() throws Exception {
        DateTimeFreezer.freezeTime();

        when(ipAddressResolver.lookupIpAddress(ENDPOINT_URL)).thenReturn(ENDPOINT_IP_ADDRESS);
        externalCommunicationEventLogger = new ExternalCommunicationEventLogger(SERVICE_INFO, eventSinkProxy, eventEmitter, ipAddressResolver);

    }

    @After
    public void tearDown() throws Exception {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void logMatchingServiceRequest_shouldPassHubEventToEventSinkProxy() {
        externalCommunicationEventLogger.logMatchingServiceRequest(MESSAGE_ID, SESSION_ID, ENDPOINT_URL);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            Map.of(
                    external_communication_type, MATCHING_SERVICE_REQUEST,
                    message_id, MESSAGE_ID,
                    external_endpoint, ENDPOINT_URL.toString(),
                    external_ip_address, ENDPOINT_IP_ADDRESS
            ));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logAuthenticationRequest_shouldPassHubEventToEventSinkProxy() {
        externalCommunicationEventLogger.logIdpAuthnRequest(MESSAGE_ID, SESSION_ID, ENDPOINT_URL, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            Map.of(
                    external_communication_type, AUTHN_REQUEST,
                    message_id, MESSAGE_ID,
                    external_endpoint, ENDPOINT_URL.toString(),
                    principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB
            ));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logResponseFromHub_shouldPassHubEventToEventSinkProxy() {
        externalCommunicationEventLogger.logResponseFromHub(MESSAGE_ID, SESSION_ID, ENDPOINT_URL, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            Map.of(
                external_communication_type, RESPONSE_FROM_HUB,
                message_id, MESSAGE_ID,
                external_endpoint, ENDPOINT_URL.toString(),
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_THE_HUB
            ));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    private static class EventMatching implements ArgumentMatcher<EventSinkHubEvent> {

        private EventSinkHubEvent expectedEvent;

        private EventMatching(EventSinkHubEvent expectedEvent) {
            this.expectedEvent = expectedEvent;
        }

        @Override
        public boolean matches(EventSinkHubEvent actualEvent) {
            if (actualEvent == null || expectedEvent.getClass() != actualEvent.getClass()) {
                return false;
            }
            return !actualEvent.getEventId().toString().isEmpty() &&
                Objects.equals(expectedEvent.getTimestamp(), actualEvent.getTimestamp()) &&
                Objects.equals(expectedEvent.getOriginatingService(), actualEvent.getOriginatingService()) &&
                Objects.equals(expectedEvent.getSessionId(), actualEvent.getSessionId()) &&
                Objects.equals(expectedEvent.getEventType(), actualEvent.getEventType()) &&
                Objects.equals(expectedEvent.getDetails(), actualEvent.getDetails());
        }
    }
}
