package uk.gov.ida.hub.samlproxy.logging;

import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEvent;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.samlproxy.SamlProxyConfiguration;
import uk.gov.ida.shared.utils.IpAddressResolver;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.ida.eventsink.EventDetailsKey.external_communication_type;
import static uk.gov.ida.eventsink.EventDetailsKey.external_endpoint;
import static uk.gov.ida.eventsink.EventDetailsKey.external_ip_address;
import static uk.gov.ida.eventsink.EventDetailsKey.message_id;
import static uk.gov.ida.eventsink.EventDetailsKey.principal_ip_address_as_seen_by_hub;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.EventTypes.EXTERNAL_COMMUNICATION_EVENT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.AUTHN_REQUEST;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.MATCHING_SERVICE_REQUEST;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.ExternalCommunicationsTypes.RESPONSE_FROM_HUB;

public class ExternalCommunicationEventLogger {

    private final ServiceInfoConfiguration serviceInfo;
    private final EventSinkProxy eventSinkProxy;
    private final EventEmitter eventEmitter;
    private final IpAddressResolver ipAddressResolver;
    private final boolean sendToRecordingSystem;

    private enum IncludeIpAddressState {
        WITH_RESOLVED_IP_ADDRESS,
        NO_RESOLVED_IP_ADDRESS
    }

    @Inject
    public ExternalCommunicationEventLogger(ServiceInfoConfiguration serviceInfo,
                                            EventSinkProxy eventSinkProxy,
                                            EventEmitter eventEmitter,
                                            IpAddressResolver ipAddressResolver,
                                            SamlProxyConfiguration configuration) {
        this.serviceInfo = serviceInfo;
        this.eventSinkProxy = eventSinkProxy;
        this.eventEmitter = eventEmitter;
        this.ipAddressResolver = ipAddressResolver;
        this.sendToRecordingSystem = configuration.getSendToRecordingSystem();
    }

    public void logMatchingServiceRequest(String messageId, SessionId sessionId, URI targetUrl) {

        logExternalCommunicationEvent(messageId, sessionId, targetUrl, MATCHING_SERVICE_REQUEST, IncludeIpAddressState.WITH_RESOLVED_IP_ADDRESS);
    }

    public void logIdpAuthnRequest(String messageId, SessionId sessionId, URI targetUrl, String principalIpAddressAsSeenByHub) {

        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressAsSeenByHub);

        logExternalCommunicationEvent(messageId, sessionId, targetUrl, AUTHN_REQUEST, IncludeIpAddressState.NO_RESOLVED_IP_ADDRESS, details);
    }

    public void logResponseFromHub(String messageId, SessionId sessionId, URI endpointUrl, String principalIpAddressAsSeenByHub) {

        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressAsSeenByHub);

        logExternalCommunicationEvent(messageId, sessionId, endpointUrl, RESPONSE_FROM_HUB, IncludeIpAddressState.NO_RESOLVED_IP_ADDRESS, details);
    }

    private void logExternalCommunicationEvent(String messageId, SessionId sessionId, URI targetUrl, String externalCommunicationType, IncludeIpAddressState includeIpAddressState) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        logExternalCommunicationEvent(messageId, sessionId, targetUrl, externalCommunicationType, includeIpAddressState, details);
    }

    private void logExternalCommunicationEvent(String messageId, SessionId sessionId, URI targetUrl, String externalCommunicationType, IncludeIpAddressState includeIpAddressState, Map<EventDetailsKey, String> details) {
        details.put(external_communication_type, externalCommunicationType);
        details.put(message_id, messageId);
        details.put(external_endpoint, targetUrl.toString());

        if(includeIpAddressState == IncludeIpAddressState.WITH_RESOLVED_IP_ADDRESS) {
            details.put(external_ip_address, ipAddressResolver.lookupIpAddress(targetUrl));
        }

        final EventSinkHubEvent hubEvent = new EventSinkHubEvent(
            serviceInfo,
            sessionId,
            EXTERNAL_COMMUNICATION_EVENT,
            details
        );
        eventSinkProxy.logHubEvent(hubEvent);
        if (sendToRecordingSystem) {
            eventEmitter.record(hubEvent);
        }
    }
}
