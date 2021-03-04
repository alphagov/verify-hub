package uk.gov.ida.hub.policy.logging;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventemitter.EventDetailsKey;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.ida.eventemitter.EventDetailsKey.ab_test_variant;
import static uk.gov.ida.eventemitter.EventDetailsKey.analytics_session_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.downstream_uri;
import static uk.gov.ida.eventemitter.EventDetailsKey.error_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.gpg45_status;
import static uk.gov.ida.eventemitter.EventDetailsKey.hub_event_type;
import static uk.gov.ida.eventemitter.EventDetailsKey.idp_entity_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.idp_fraud_event_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.journey_type;
import static uk.gov.ida.eventemitter.EventDetailsKey.maximum_level_of_assurance;
import static uk.gov.ida.eventemitter.EventDetailsKey.message;
import static uk.gov.ida.eventemitter.EventDetailsKey.message_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.minimum_level_of_assurance;
import static uk.gov.ida.eventemitter.EventDetailsKey.pid;
import static uk.gov.ida.eventemitter.EventDetailsKey.preferred_level_of_assurance;
import static uk.gov.ida.eventemitter.EventDetailsKey.principal_ip_address_as_seen_by_hub;
import static uk.gov.ida.eventemitter.EventDetailsKey.principal_ip_address_as_seen_by_idp;
import static uk.gov.ida.eventemitter.EventDetailsKey.provided_level_of_assurance;
import static uk.gov.ida.eventemitter.EventDetailsKey.request_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.session_event_type;
import static uk.gov.ida.eventemitter.EventDetailsKey.session_expiry_time;
import static uk.gov.ida.eventemitter.EventDetailsKey.transaction_entity_id;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.HUB_EVENT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.SESSION_EVENT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.HubEvents.RECEIVED_AUTHN_REQUEST_FROM_HUB;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE01_MATCH;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE01_NO_MATCH;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_CANCEL;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_DATA_OBTAINED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_MATCH;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_NO_MATCH;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.FRAUD_DETECTED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_FAILED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_PENDING;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_SUCCEEDED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_SELECTED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.NO_AUTHN_CONTEXT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.REQUESTER_ERROR;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.SESSION_STARTED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.SESSION_TIMEOUT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATION_FAILED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATION_REQUEST_SENT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.WAITING_FOR_CYCLE3_ATTRIBUTES;

public class HubEventLogger {

    private static final Logger LOG = LoggerFactory.getLogger(HubEventLogger.class);

    private final EventSinkProxy eventSinkProxy;
    private final ServiceInfoConfiguration serviceInfo;
    private final EventEmitter eventEmitter;

    @Inject
    public HubEventLogger(ServiceInfoConfiguration serviceInfo,
                          EventSinkProxy eventSinkProxy,
                          EventEmitter eventEmitter) {
        this.serviceInfo = serviceInfo;
        this.eventSinkProxy = eventSinkProxy;
        this.eventEmitter = eventEmitter;
    }

    public void logSessionStartedEvent(SamlResponseWithAuthnRequestInformationDto samlResponse, String ipAddress, DateTime sessionExpiryTimestamp, SessionId sessionId, LevelOfAssurance minimum, LevelOfAssurance maximum, LevelOfAssurance preferred) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, ipAddress);
        details.put(message_id, samlResponse.getId());
        details.put(minimum_level_of_assurance, minimum.name());
        details.put(maximum_level_of_assurance, maximum.name());
        details.put(preferred_level_of_assurance, preferred.name());

        logSessionEvent(sessionId, samlResponse.getIssuer(), sessionExpiryTimestamp, samlResponse.getId(), SESSION_STARTED, details);
    }

    public void logIdpAuthnFailedEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp,
                                       String requestId, String principalIpAddressSeenByHub, String analyticsSessionId,
                                       String journeyType, String idpEntityID) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressSeenByHub);
        details.put(idp_entity_id, idpEntityID);
        details.put(analytics_session_id, analyticsSessionId);
        details.put(journey_type, journeyType);
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

    public void logNoAuthnContextEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, String principalIpAddressAsSeenByHub, String analyticsSessionId, String journeyType, String idpEntityID) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(idp_entity_id, idpEntityID);
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressAsSeenByHub);
        details.put(analytics_session_id, analyticsSessionId);
        details.put(journey_type, journeyType);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, NO_AUTHN_CONTEXT, details);
    }

    public void logPausedRegistrationEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, String principalIdAsSeenByHub, String analyticsSessionId, String journeyType, String idpEntityID) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, principalIdAsSeenByHub);
        details.put(idp_entity_id, idpEntityID);
        details.put(analytics_session_id, analyticsSessionId);
        details.put(journey_type, journeyType);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, IDP_AUTHN_PENDING, details);
    }

    public void logIdpFraudEvent(SessionId sessionId, String idpEntityID, String requestIssuerEntityID, PersistentId persistentId, DateTime sessionExpiryTimestamp, FraudDetectedDetails fraudDetectedDetails,
                                 Optional<String> principalIpAddressSeenByIdp, String principalIpAddressSeenByHub, String requestId, String analyticsSessionId, String journeyType) {
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(idp_entity_id, idpEntityID);
        details.put(pid, persistentId.getNameId());
        details.put(idp_fraud_event_id, fraudDetectedDetails.getIdpFraudEventId());
        details.put(gpg45_status, fraudDetectedDetails.getFraudIndicator());
        details.put(analytics_session_id, analyticsSessionId);
        details.put(journey_type, journeyType);

        if (principalIpAddressSeenByIdp.isPresent()) {
            details.put(principal_ip_address_as_seen_by_idp, principalIpAddressSeenByIdp.get());
        }
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressSeenByHub);

        logSessionEvent(sessionId, requestIssuerEntityID, sessionExpiryTimestamp, requestId, FRAUD_DETECTED, details);
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

    public void logIdpRequesterErrorEvent(SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId, Optional<String> errorMessage, String principalIpAddressSeenByHub, String analyticsSessionId, String journeyType,
  String idpEntityId) {
        EnumMap<EventDetailsKey, String> details = new EnumMap<>(EventDetailsKey.class);
        if (errorMessage.isPresent()) {
            details.put(message, errorMessage.get());
        }
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressSeenByHub);
        details.put(analytics_session_id, analyticsSessionId);
        details.put(idp_entity_id, idpEntityId);
        details.put(journey_type, journeyType);
        logSessionEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, REQUESTER_ERROR, details);
    }

    public void logIdpAuthnSucceededEvent(SessionId sessionId,
                                          DateTime sessionExpiryTimestamp,
                                          String idpEntityId,
                                          String transactionEntityId,
                                          PersistentId persistentId,
                                          String requestId,
                                          LevelOfAssurance minimumLevelOfAssurance,
                                          LevelOfAssurance maximumLevelOfAssurance,
                                          LevelOfAssurance preferredLevelOfAssurance,
                                          LevelOfAssurance providedLevelOfAssurance,
                                          Optional<String> principalIpAddress,
                                          String principalIpAddressAsSeenByHub,
                                          String analyticsSessionId,
                                          String journeyType) {

        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(idp_entity_id, idpEntityId);
        details.put(pid, persistentId.getNameId());
        details.put(minimum_level_of_assurance, minimumLevelOfAssurance.name());
        details.put(maximum_level_of_assurance, maximumLevelOfAssurance.name());
        details.put(preferred_level_of_assurance, preferredLevelOfAssurance.name());
        details.put(provided_level_of_assurance, providedLevelOfAssurance.name());
        if (principalIpAddress.isPresent()) {
            details.put(principal_ip_address_as_seen_by_idp, principalIpAddress.get());
        }
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddressAsSeenByHub);
        details.put(analytics_session_id, analyticsSessionId);
        details.put(journey_type, journeyType);
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

    public void logIdpSelectedEvent(IdpSelectedState idpSelectedState, String principalIpAddress, String analyticsSessionId, String journeyType, String abTestVariant) {
        List<LevelOfAssurance> levelsOfAssurance = idpSelectedState.getLevelsOfAssurance();
        Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(idp_entity_id, idpSelectedState.getIdpEntityId());
        details.put(principal_ip_address_as_seen_by_hub, principalIpAddress);
        details.put(minimum_level_of_assurance, Collections.min(levelsOfAssurance).name());
        details.put(maximum_level_of_assurance, Collections.max(levelsOfAssurance).name());
        details.put(preferred_level_of_assurance, levelsOfAssurance.get(0).name());
        details.put(analytics_session_id, analyticsSessionId);
        details.put(journey_type, journeyType);
        details.put(ab_test_variant, abTestVariant);

        logSessionEvent(
            idpSelectedState.getSessionId(),
            idpSelectedState.getRequestIssuerEntityId(),
            idpSelectedState.getSessionExpiryTimestamp(),
            idpSelectedState.getRequestId(),
            IDP_SELECTED,
            details);
    }

    public void logSessionMovedToStartStateEvent(SessionStartedState state) {
        logSessionEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getSessionExpiryTimestamp(), state.getRequestId(), SESSION_STARTED, new HashMap<>());
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

    public void logErrorEvent(final UUID errorId, final SessionId sessionId, final String errorMessage) {
        final Map<EventDetailsKey, String> details = Map.of(
            message, errorMessage,
            error_id, errorId.toString());
        logErrorEvent(details, sessionId);
    }

    public void logErrorEvent(final UUID errorId, final String entityId, final SessionId sessionId) {
        final Map<EventDetailsKey, String> details = Map.of(
            idp_entity_id, entityId,
            error_id, errorId.toString());
        logErrorEvent(details, sessionId);
    }

    public void logErrorEvent(final UUID errorId, final SessionId sessionId, final String errorMessage, final String downstreamUri) {
        final Map<EventDetailsKey, String> details = Map.of(
            downstream_uri, downstreamUri,
            message, errorMessage,
            error_id, errorId.toString());
        logErrorEvent(details, sessionId);
    }

    public void logErrorEvent(final String errorMessage, final SessionId sessionId) {
        final Map<EventDetailsKey, String> details = Map.of(
            message, errorMessage);
        logErrorEvent(details, sessionId);
    }

    private void logErrorEvent(final Map<EventDetailsKey, String> details, final SessionId sessionId) {
        final EventSinkHubEvent eventSinkHubEvent = new EventSinkHubEvent(
            serviceInfo,
            sessionId,
            EventSinkHubEventConstants.EventTypes.ERROR_EVENT,
            details
        );
        eventSinkProxy.logHubEvent(eventSinkHubEvent);
        eventEmitter.record(eventSinkHubEvent);
    }
}
