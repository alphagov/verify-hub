package uk.gov.ida.hub.samlsoapproxy.logging;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.ServiceInfoConfigurationBuilder;
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
import static uk.gov.ida.eventemitter.EventDetailsKey.external_communication_type;
import static uk.gov.ida.eventemitter.EventDetailsKey.external_endpoint;
import static uk.gov.ida.eventemitter.EventDetailsKey.external_ip_address;
import static uk.gov.ida.eventemitter.EventDetailsKey.message_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.principal_ip_address_as_seen_by_hub;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.EXTERNAL_COMMUNICATION_EVENT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.AUTHN_REQUEST;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.MATCHING_SERVICE_REQUEST;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.RESPONSE_FROM_HUB;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ExternalCommunicationEventLoggerTest {

    private static final ServiceInfoConfiguration SERVICE_INFO = ServiceInfoConfigurationBuilder.aServiceInfo().withName("some-service-name").build();
    private static final SessionId SESSION_ID = SessionId.createNewSessionId();
    private static final String MESSAGE_ID = "some-message-id";
    private static final URI ENDPOINT_URL = URI.create("http://someurl.com");
    private static final String ENDPOINT_IP_ADDRESS = "1.2.3.4";
    private static final String PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB = "some-ip-address";

    @Mock
    EventSinkProxy eventSinkProxy;

    @Mock
    EventEmitter eventEmitter;

    @Mock
    IpAddressResolver ipAddressResolver;

    private ExternalCommunicationEventLogger externalCommunicationEventLogger;

    @BeforeEach
    public void setUp() {
        when(ipAddressResolver.lookupIpAddress(ENDPOINT_URL)).thenReturn(ENDPOINT_IP_ADDRESS);
        externalCommunicationEventLogger = new ExternalCommunicationEventLogger(SERVICE_INFO, eventSinkProxy, eventEmitter, ipAddressResolver);
        DateTimeFreezer.freezeTime();
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void logMatchingServiceRequest_shouldPassHubEventToEventSinkProxyNew() {
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
        externalCommunicationEventLogger.logIdpAuthnRequest(MESSAGE_ID, SESSION_ID, ENDPOINT_URL, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            Map.of(
                    external_communication_type, AUTHN_REQUEST,
                    message_id, MESSAGE_ID,
                    external_endpoint, ENDPOINT_URL.toString(),
                    principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB
            ));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logResponseFromHub_shouldPassHubEventToEventSinkProxy() {
        externalCommunicationEventLogger.logResponseFromHub(MESSAGE_ID, SESSION_ID, ENDPOINT_URL, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            EXTERNAL_COMMUNICATION_EVENT,
            Map.of(
                    external_communication_type, RESPONSE_FROM_HUB,
                    message_id, MESSAGE_ID,
                    external_endpoint, ENDPOINT_URL.toString(),
                    principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB
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
        public boolean matches(EventSinkHubEvent argument) {
            if (argument == null || expectedEvent.getClass() != argument.getClass()) {
                return false;
            }
            return !argument.getEventId().toString().isEmpty() &&
                Objects.equals(expectedEvent.getTimestamp(), argument.getTimestamp()) &&
                Objects.equals(expectedEvent.getOriginatingService(), argument.getOriginatingService()) &&
                Objects.equals(expectedEvent.getSessionId(), argument.getSessionId()) &&
                Objects.equals(expectedEvent.getEventType(), argument.getEventType()) &&
                Objects.equals(expectedEvent.getDetails(), argument.getDetails());
        }
    }
}
