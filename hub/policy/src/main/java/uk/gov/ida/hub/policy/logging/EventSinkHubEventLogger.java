package uk.gov.ida.hub.policy.logging;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.proxy.EventSinkProxy;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.gov.ida.eventsink.EventDetailsKey.gpg45_status;
import static uk.gov.ida.eventsink.EventDetailsKey.hub_event_type;
import static uk.gov.ida.eventsink.EventDetailsKey.idp_entity_id;
import static uk.gov.ida.eventsink.EventDetailsKey.idp_fraud_event_id;
import static uk.gov.ida.eventsink.EventDetailsKey.message;
import static uk.gov.ida.eventsink.EventDetailsKey.message_id;
import static uk.gov.ida.eventsink.EventDetailsKey.minimum_level_of_assurance;
import static uk.gov.ida.eventsink.EventDetailsKey.pid;
import static uk.gov.ida.eventsink.EventDetailsKey.principal_ip_address_as_seen_by_hub;
import static uk.gov.ida.eventsink.EventDetailsKey.principal_ip_address_as_seen_by_idp;
import static uk.gov.ida.eventsink.EventDetailsKey.provided_level_of_assurance;
import static uk.gov.ida.eventsink.EventDetailsKey.request_id;
import static uk.gov.ida.eventsink.EventDetailsKey.required_level_of_assurance;
import static uk.gov.ida.eventsink.EventDetailsKey.session_event_type;
import static uk.gov.ida.eventsink.EventDetailsKey.session_expiry_time;
import static uk.gov.ida.eventsink.EventDetailsKey.transaction_entity_id;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.EventTypes.HUB_EVENT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.EventTypes.SESSION_EVENT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.HubEvents.RECEIVED_AUTHN_REQUEST_FROM_HUB;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.COUNTRY_SELECTED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE01_MATCH;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE01_NO_MATCH;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_CANCEL;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_DATA_OBTAINED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_MATCH;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_NO_MATCH;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.FRAUD_DETECTED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_FAILED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_PENDING;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_SUCCEEDED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_SELECTED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.NO_AUTHN_CONTEXT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.REQUESTER_ERROR;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.SESSION_STARTED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.SESSION_TIMEOUT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATION_FAILED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATION_REQUEST_SENT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.WAITING_FOR_CYCLE3_ATTRIBUTES;

public class EventSinkHubEventLogger {

    private static final Logger LOG = LoggerFactory.getLogger(EventSinkHubEventLogger.class);

    private final EventSinkProxy eventSinkProxy;
    private final ServiceInfoConfiguration serviceInfo;
    private final EventEmitter eventEmitter;

    @Inject
    public EventSinkHubEventLogger(ServiceInfoConfiguration serviceInfo, EventSinkProxy eventSinkProxy, EventEmitter eventEmitter) {
        this.serviceInfo = serviceInfo;
        this.eventSinkProxy = eventSinkProxy;
        this.eventEmitter = eventEmitter;
    }

    public void logSessionStartedEvent(SamlResponseWithAuthnRequestInformationDto samlResponse, String ipAddress, DateTime sessionExpiryTimestamp, SessionId sessionId, LevelOfAssurance minimum, LevelOfAssurance required) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, ipAddress);
        details.put(message_id, samlResponse.getId());
        details.put(minimum_level_of_assurance, minimum.name());
        details.put(required_level_of_assurance, required.name());

        logSessionEvent(sessionId, samlResponse.getIssuer(), sessionExpiryTimestamp, samlResponse.getId(), SESSION_STARTED, details);
    }

    public void logIdpAuthnFailedEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, String principalIpAddressSeenByHub) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressSeenByHub);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, IDP_AUTHN_FAILED, details);
    }

    public void logCycle3SuccessfulMatchEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, CYCLE3_MATCH, new HashMap<>());
    }

    public void logCycle3NoMatchEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, CYCLE3_NO_MATCH, new HashMap<>());
    }

    public void logCycle01SuccessfulMatchEvent(SessionId sessionId, String transactionEntityId, String requestId, DateTime sessionExpiryTimestamp) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, CYCLE01_MATCH, new HashMap<>());
    }

    public void logCycle01NoMatchEvent(SessionId sessionId, String transactionEntityId, String requestId, DateTime sessionExpiryTimestamp) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, CYCLE01_NO_MATCH, new HashMap<>());
    }

    public void logUserAccountCreatedEvent(final SessionId sessionId, final String transactionEntityId, final String requestId, final DateTime sessionExpiryTimestamp) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, USER_ACCOUNT_CREATED, new HashMap<>());
    }

    public void logUserAccountCreationFailedEvent(final SessionId sessionId, final String transactionEntityId, final String requestId, final DateTime sessionExpiryTimestamp) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, USER_ACCOUNT_CREATION_FAILED, new HashMap<>());
    }

    public void logNoAuthnContextEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, String principalIpAddressAsSeenByHub) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressAsSeenByHub);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, NO_AUTHN_CONTEXT, details);
    }

    public void logPausedRegistrationEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, String principalIdAsSeenByHub) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, principalIdAsSeenByHub);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, IDP_AUTHN_PENDING, details);
    }

    public void logIdpFraudEvent(SessionId sessionId, String transactionEntityId, String idpEntityId, PersistentId persistentId, DateTime sessionExpiryTimestamp, FraudDetectedDetails fraudDetectedDetails,
                                 Optional<String> principalIpAddressSeenByIdp, String principalIpAddressSeenByHub, String requestId) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(idp_entity_id, transactionEntityId);
        details.put(pid, persistentId.getNameId());
        details.put(idp_fraud_event_id, fraudDetectedDetails.getIdpFraudEventId());
        details.put(gpg45_status, fraudDetectedDetails.getFraudIndicator());
        if (principalIpAddressSeenByIdp.isPresent()) {
            details.put(principal_ip_address_as_seen_by_idp, principalIpAddressSeenByIdp.get());
        }
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressSeenByHub);

        logSessionEvent(sessionId, idpEntityId, sessionExpiryTimestamp, requestId, FRAUD_DETECTED, details);
    }

    public void logWaitingForCycle3AttributesEvent(SessionId sessionId, String transactionEntityId, String requestId, DateTime sessionExpiryTimestamp) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, WAITING_FOR_CYCLE3_ATTRIBUTES, new HashMap<>());
    }

    public void logCycle3DataObtained(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, String principalIpAddressAsSeenByHub) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressAsSeenByHub);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, CYCLE3_DATA_OBTAINED, details);
    }

    public void logCycle3DataInputCancelled(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, CYCLE3_CANCEL, new HashMap<>());
    }

    public void logIdpRequesterErrorEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, Optional<String> errorMessage, String principalIpAddressSeenByHub) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        if (errorMessage.isPresent()) {
            details.put(message, errorMessage.get());
        }
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressSeenByHub);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, REQUESTER_ERROR, details);
    }

    public void logIdpAuthnSucceededEvent(SessionId sessionId, DateTime sessionExpiryTimestamp, String idpEntityId, String transactionEntityId, PersistentId persistentId, String requestId, LevelOfAssurance minimumLevelOfAssurance, LevelOfAssurance requiredLevelOfAssurance, LevelOfAssurance providedLevelOfAssurance, Optional<String> principalIpAddress, String principalIpAddressAsSeenByHub) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(idp_entity_id, idpEntityId);
        details.put(pid, persistentId.getNameId());
        details.put(minimum_level_of_assurance, minimumLevelOfAssurance.name());
        details.put(required_level_of_assurance, requiredLevelOfAssurance.name());
        details.put(provided_level_of_assurance, providedLevelOfAssurance.name());
        if (principalIpAddress.isPresent()) {
            details.put(principal_ip_address_as_seen_by_idp, principalIpAddress.get());
        }
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressAsSeenByHub);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, IDP_AUTHN_SUCCEEDED, details);
    }

    public void logRequestFromHub(SessionId sessionId, String transactionEntityId) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(hub_event_type, RECEIVED_AUTHN_REQUEST_FROM_HUB);
        details.put(transaction_entity_id, transactionEntityId);

        EventSinkHubEvent eventSinkHubEvent = new EventSinkHubEvent(
                serviceInfo,
                sessionId,
                HUB_EVENT,
                details);

        eventSinkProxy.logHubEvent(eventSinkHubEvent);
        eventEmitter.record(eventSinkHubEvent);
    }

    public void logSessionTimeoutEvent(SessionId sessionId, DateTime sessionExpiryTimestamp, String transactionEntityId, String requestId) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(session_event_type, SESSION_TIMEOUT);
        details.put(session_expiry_time, sessionExpiryTimestamp.toString());
        details.put(transaction_entity_id, transactionEntityId);
        details.put(request_id, requestId);

        EventSinkHubEvent eventSinkHubEvent = new EventSinkHubEvent(
                serviceInfo,
                sessionId,
                SESSION_EVENT,
                details);

        eventSinkProxy.logHubEvent(eventSinkHubEvent);
        eventEmitter.record(eventSinkHubEvent);
    }

    public void logMatchingServiceUserAccountCreationRequestSentEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId) {
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, USER_ACCOUNT_CREATION_REQUEST_SENT, new HashMap<>());
    }


    public void logIdpSelectedEvent(IdpSelectedState idpSelectedState, String principalIpAddress) {
        List<LevelOfAssurance> levelsOfAssurance = idpSelectedState.getLevelsOfAssurance();
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(idp_entity_id, idpSelectedState.getIdpEntityId());
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddress);
        details.put(minimum_level_of_assurance, levelsOfAssurance.get(0).name());
        details.put(required_level_of_assurance, levelsOfAssurance.get(levelsOfAssurance.size() - 1).name());
        logSessionEvent(idpSelectedState.getSessionId(), idpSelectedState.getRequestIssuerEntityId(), idpSelectedState.getSessionExpiryTimestamp(), idpSelectedState.getRequestId(), IDP_SELECTED, details);
    }

    private void logSessionEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, String state, Map<EventDetailsKey, String> details) {
        LOG.info(MessageFormat.format("Session {0} moved to state {1}", sessionId.getSessionId(), state));
        details.put(session_event_type, state);
        details.put(session_expiry_time, sessionExpiryTimestamp.toString());
        details.put(transaction_entity_id, transactionEntityId);
        details.put(request_id, requestId);

        EventSinkHubEvent sessionHubEvent = new EventSinkHubEvent(
                serviceInfo,
                sessionId,
                SESSION_EVENT,
                details);

        eventSinkProxy.logHubEvent(sessionHubEvent);
        eventEmitter.record(sessionHubEvent);
    }

    public void logCountrySelectedEvent(CountrySelectedState countrySelectedState) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(transaction_entity_id, countrySelectedState.getRequestIssuerEntityId());
        details.put(EventDetailsKey.country_code, countrySelectedState.getCountryEntityId());
        logSessionEvent(countrySelectedState.getSessionId(), countrySelectedState.getRequestIssuerEntityId(), countrySelectedState.getSessionExpiryTimestamp(), countrySelectedState.getRequestId(), COUNTRY_SELECTED, details);
    }
}
