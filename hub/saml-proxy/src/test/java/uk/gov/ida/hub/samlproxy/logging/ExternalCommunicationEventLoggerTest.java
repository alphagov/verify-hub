package uk.gov.ida.hub.samlproxy.logging;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEvent;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.shared.utils.IpAddressResolver;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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

    @Mock
    EventSinkProxy eventSinkProxy;

    @Mock
    IpAddressResolver ipAddressResolver;

    @Before
    public void setUp() throws Exception {
        DateTimeFreezer.freezeTime();
    }

    @After
    public void tearDown() throws Exception {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void logMatchingServiceRequest_shouldPassHubEventToEventSinkProxy() {

        final String serviceName = "some-service-name";
        final SessionId sessionId = SessionId.createNewSessionId();
        final String messageId = "some-message-id";
        final String endpoint = "http://someurl.com";
        final URI endpointUrl = URI.create(endpoint);

        final String endpointIpAddress = "1.2.3.4";
        when(ipAddressResolver.lookupIpAddress(endpointUrl)).thenReturn(endpointIpAddress);

        final ExternalCommunicationEventLogger externalCommunicationEventLogger = new ExternalCommunicationEventLogger(aServiceInfo().withName(serviceName).build(), eventSinkProxy, ipAddressResolver);
        externalCommunicationEventLogger.logMatchingServiceRequest(messageId, sessionId, endpointUrl);

        final ArgumentCaptor<EventSinkHubEvent> logHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(logHubEvent.capture());

        final EventSinkHubEvent loggedEvent = logHubEvent.getValue();
        assertThat(loggedEvent.getOriginatingService()).isEqualTo(serviceName);
        assertThat(loggedEvent.getEventType()).isEqualTo(EXTERNAL_COMMUNICATION_EVENT);
        assertThat(loggedEvent.getTimestamp().isEqual(DateTime.now())).isTrue();
        assertThat(loggedEvent.getSessionId()).isEqualTo(sessionId.toString());
        final Map<EventDetailsKey, String> eventDetails = loggedEvent.getDetails();
        assertThat(eventDetails.get(external_communication_type)).isEqualTo(MATCHING_SERVICE_REQUEST);
        assertThat(eventDetails.get(message_id)).isEqualTo(messageId);
        assertThat(eventDetails.get(external_endpoint)).isEqualTo(endpoint);
        assertThat(eventDetails.get(external_ip_address)).isEqualTo(endpointIpAddress);
    }

    @Test
    public void logAuthenticationRequest_shouldPassHubEventToEventSinkProxy() {

        final String serviceName = "some-service-name";
        final SessionId sessionId = SessionId.createNewSessionId();
        final String messageId = "some-message-id";
        final String endpoint = "http://someurl.com";
        final URI endpointUrl = URI.create(endpoint);
        final String principalIpAddress = "some-ip-address";

        final String endpointIpAddress = "1.2.3.4";
        when(ipAddressResolver.lookupIpAddress(endpointUrl)).thenReturn(endpointIpAddress);

        final ExternalCommunicationEventLogger externalCommunicationEventLogger = new ExternalCommunicationEventLogger(aServiceInfo().withName(serviceName).build(), eventSinkProxy, ipAddressResolver);
        externalCommunicationEventLogger.logIdpAuthnRequest(messageId, sessionId, endpointUrl, principalIpAddress);

        final ArgumentCaptor<EventSinkHubEvent> logHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(logHubEvent.capture());

        final EventSinkHubEvent loggedEvent = logHubEvent.getValue();
        assertThat(loggedEvent.getOriginatingService()).isEqualTo(serviceName);
        assertThat(loggedEvent.getEventType()).isEqualTo(EXTERNAL_COMMUNICATION_EVENT);
        assertThat(loggedEvent.getTimestamp().isEqual(DateTime.now())).isTrue();
        assertThat(loggedEvent.getSessionId()).isEqualTo(sessionId.toString());
        final Map<EventDetailsKey, String> eventDetails = loggedEvent.getDetails();
        assertThat(eventDetails.get(external_communication_type)).isEqualTo(AUTHN_REQUEST);
        assertThat(eventDetails.get(message_id)).isEqualTo(messageId);
        assertThat(eventDetails.get(external_endpoint)).isEqualTo(endpoint);
        assertThat(eventDetails.get(external_ip_address)).isNull();
        assertThat(eventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddress);
    }

    @Test
    public void logResponseFromHub_shouldPassHubEventToEventSinkProxy() {

        final String serviceName = "some-service-name";
        final SessionId sessionId = SessionId.createNewSessionId();
        final String messageId = "some-message-id";
        final String endpoint = "http://someurl.com";
        final URI endpointUrl = URI.create(endpoint);

        final String endpointIpAddress = "1.2.3.4";
        when(ipAddressResolver.lookupIpAddress(endpointUrl)).thenReturn(endpointIpAddress);

        final ExternalCommunicationEventLogger externalCommunicationEventLogger = new ExternalCommunicationEventLogger(aServiceInfo().withName(serviceName).build(), eventSinkProxy, ipAddressResolver);
        final String principalIpAddressSeenByHub = "a-principal-ip-address";
        externalCommunicationEventLogger.logResponseFromHub(messageId, sessionId, endpointUrl, principalIpAddressSeenByHub);

        final ArgumentCaptor<EventSinkHubEvent> logHubEvent = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy).logHubEvent(logHubEvent.capture());

        final EventSinkHubEvent loggedEvent = logHubEvent.getValue();
        assertThat(loggedEvent.getOriginatingService()).isEqualTo(serviceName);
        assertThat(loggedEvent.getEventType()).isEqualTo(EXTERNAL_COMMUNICATION_EVENT);
        assertThat(loggedEvent.getTimestamp().isEqual(DateTime.now())).isTrue();
        assertThat(loggedEvent.getSessionId()).isEqualTo(sessionId.toString());
        assertThat(loggedEvent.getSessionId()).isEqualTo(sessionId.toString());
        final Map<EventDetailsKey, String> eventDetails = loggedEvent.getDetails();
        assertThat(eventDetails.get(external_communication_type)).isEqualTo(RESPONSE_FROM_HUB);
        assertThat(eventDetails.get(message_id)).isEqualTo(messageId);
        assertThat(eventDetails.get(external_endpoint)).isEqualTo(endpoint);
        assertThat(eventDetails.get(external_ip_address)).isNull();
        assertThat(eventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddressSeenByHub);
    }
}
