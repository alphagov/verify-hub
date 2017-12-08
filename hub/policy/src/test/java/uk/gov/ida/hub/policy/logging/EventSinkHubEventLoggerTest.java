package uk.gov.ida.hub.policy.logging;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.ServiceInfoConfigurationBuilder;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.proxy.EventSinkProxy;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.eventsink.EventDetailsKey.gpg45_status;
import static uk.gov.ida.eventsink.EventDetailsKey.hub_event_type;
import static uk.gov.ida.eventsink.EventDetailsKey.idp_entity_id;
import static uk.gov.ida.eventsink.EventDetailsKey.idp_fraud_event_id;
import static uk.gov.ida.eventsink.EventDetailsKey.message;
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
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_DATA_OBTAINED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.FRAUD_DETECTED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_FAILED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_PENDING;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_SELECTED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.NO_AUTHN_CONTEXT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.REQUESTER_ERROR;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.SESSION_STARTED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.SESSION_TIMEOUT;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATION_REQUEST_SENT;
import static uk.gov.ida.hub.policy.builder.domain.FraudDetectedDetailsBuilder.aFraudDetectedDetails;
import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto;

@RunWith(MockitoJUnitRunner.class)
public class EventSinkHubEventLoggerTest {

    @Mock
    private EventSinkProxy eventSinkProxy;
    ServiceInfoConfiguration serviceInfo = ServiceInfoConfigurationBuilder.aServiceInfo().withName("service-name").build();

    @After
    public void unfreezeTime() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void logSessionOpenEvent_shouldSendEvent() throws Exception {
        DateTimeFreezer.freezeTime();
        int sessionDuration = 10;
        DateTime expectedSessionExpiryTime = DateTime.now().plusMinutes(sessionDuration);

        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        SessionId expectedSessionId = aSessionId().build();
        String principalIpAddress = "some-ip-address";
        String requestId = UUID.randomUUID().toString();
        LevelOfAssurance minimumLevelOfAssurance = LevelOfAssurance.LEVEL_1;
        LevelOfAssurance requiredLevelOfAssurance = LevelOfAssurance.LEVEL_2;
        SamlResponseWithAuthnRequestInformationDto samlResponse = aSamlResponseWithAuthnRequestInformationDto()
                .withId(requestId)
                .build();
        eventLogger.logSessionStartedEvent(samlResponse, principalIpAddress, expectedSessionExpiryTime, expectedSessionId, minimumLevelOfAssurance, requiredLevelOfAssurance);
        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());

        EventSinkHubEvent actualEvent = eventCaptor.getValue();

        assertThat(actualEvent.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(actualEvent.getOriginatingService()).isEqualTo(serviceInfo.getName());
        assertThat(actualEvent.getSessionId()).isEqualTo(expectedSessionId.getSessionId());
        assertThat(actualEvent.getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.SESSION_EVENT);
        assertThat(actualEvent.getDetails().get(EventDetailsKey.minimum_level_of_assurance)).isEqualTo(minimumLevelOfAssurance.name());
        assertThat(actualEvent.getDetails().get(EventDetailsKey.required_level_of_assurance)).isEqualTo(requiredLevelOfAssurance.name());

        Map<EventDetailsKey, String> details = actualEvent.getDetails();

        assertThat(details.containsKey(session_event_type)).isTrue();
        String state = details.get(session_event_type);
        assertThat(state).isEqualTo(SESSION_STARTED);

        assertThat(details.containsKey(session_expiry_time)).isTrue();
        assertThat(details.get(session_expiry_time)).isEqualTo(expectedSessionExpiryTime.toString());
        assertThat(details.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddress);
    }

    @Test
    public void logEventSinkHubEvent_shouldSendEvent() throws Exception {
        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        SessionId expectedSessionId = aSessionId().build();

        eventLogger.logRequestFromHub(expectedSessionId, "transaction-entity-id");
        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());

        EventSinkHubEvent actualEvent = eventCaptor.getValue();

        assertThat(actualEvent.getEventType()).isEqualTo(HUB_EVENT);
        assertThat(actualEvent.getSessionId()).isEqualTo(expectedSessionId.getSessionId());
        Map<EventDetailsKey, String> details = actualEvent.getDetails();

        assertThat(details.containsKey(hub_event_type)).isTrue();
        String hubEventType = details.get(hub_event_type);
        assertThat(hubEventType).isEqualTo(RECEIVED_AUTHN_REQUEST_FROM_HUB);
    }

    @Test
    public void logIdpAuthnSucceededEvent_shouldContainLevelsOfAssuranceInDetailsFieldForBilling() throws Exception {

        String requestId = "requestId";
        DateTimeFreezer.freezeTime();
        int sessionDuration = 10;
        DateTime sessionExpiryTime = DateTime.now().plusMinutes(sessionDuration);
        SessionId sessionId = aSessionId().build();
        String idpEntityId = "idp entity id";
        String transactionEntityId = "transaction entity id";
        LevelOfAssurance minimumLevelOfAssurance = LevelOfAssurance.LEVEL_1;
        LevelOfAssurance requiredLevelOfAssurance = LevelOfAssurance.LEVEL_2;
        LevelOfAssurance providedLevelOfAssurance = LevelOfAssurance.LEVEL_2;
        PersistentId persistentId = aPersistentId().withNameId("nameId").build();
        Optional<String> principalIpAddress = Optional.fromNullable("principal ip address");
        String principalIpAddressAsSeenByHub = "principal-ip-address-seen-by-hub";

        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);

        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        //When
        eventLogger.logIdpAuthnSucceededEvent(sessionId, sessionExpiryTime, idpEntityId, transactionEntityId, persistentId, requestId, minimumLevelOfAssurance, requiredLevelOfAssurance, providedLevelOfAssurance, principalIpAddress, principalIpAddressAsSeenByHub);

        //Then
        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());

        EventSinkHubEvent actualEvent = eventCaptor.getValue();

        assertThat(actualEvent.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(actualEvent.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(actualEvent.getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.SESSION_EVENT);

        Map<EventDetailsKey, String> details = actualEvent.getDetails();

        assertThat(details.containsKey(minimum_level_of_assurance)).isTrue();
        String actualMinimumLevelOfAssurance = details.get(minimum_level_of_assurance);
        assertThat(actualMinimumLevelOfAssurance).isEqualTo(minimumLevelOfAssurance.name());

        assertThat(details.containsKey(required_level_of_assurance)).isTrue();
        String actualRequiredLevelOfAssurance = details.get(required_level_of_assurance);
        assertThat(actualRequiredLevelOfAssurance).isEqualTo(requiredLevelOfAssurance.name());

        assertThat(details.containsKey(provided_level_of_assurance)).isTrue();
        String actualProvidedLevelOfAssurance = details.get(provided_level_of_assurance);
        assertThat(actualProvidedLevelOfAssurance).isEqualTo(providedLevelOfAssurance.name());

        assertThat(details.containsKey(principal_ip_address_as_seen_by_idp)).isTrue();
        String actualPrincipalIpAddress = details.get(principal_ip_address_as_seen_by_idp);
        assertThat(actualPrincipalIpAddress).isEqualTo(principalIpAddress.get());

        assertThat(details.containsKey(principal_ip_address_as_seen_by_hub)).isTrue();
        assertThat(details.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddressAsSeenByHub);

        assertThat(details.containsKey(request_id)).isTrue();
        assertThat(details.get(request_id)).isEqualTo(requestId);
    }

    @Test
    public void logIdpFraudEvent_shouldLogFraudEventWithDetails() {
        DateTimeFreezer.freezeTime();
        SessionId sessionId = aSessionId().build();
        String idpEntityId = "idp";
        String transactionEntityId = "transaction entity id";
        PersistentId persistentIdDto = aPersistentId().withNameId("nameId").build();
        DateTime sessionExpiryTime = DateTime.now().plusMinutes(10);
        String fraudEventId = "fraudEventId";
        String fraudIndicator = "FI02";
        String principalIpAddressSeenByIdp = "principal-ip-address-seen-by-idp";
        String principalIpAddressSeenByHub = "principal-ip-address-seen-by-hub";
        FraudDetectedDetails fraudDetectedDetailsDto = aFraudDetectedDetails()
                .withFraudEventId(fraudEventId)
                .withFraudIndicator(fraudIndicator)
                .build();
        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        String requestId = UUID.randomUUID().toString();

        eventLogger.logIdpFraudEvent(sessionId, idpEntityId, transactionEntityId, persistentIdDto, sessionExpiryTime, fraudDetectedDetailsDto, Optional.fromNullable(principalIpAddressSeenByIdp), principalIpAddressSeenByHub, requestId);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());

        EventSinkHubEvent actualHubEvent = eventCaptor.getValue();
        Map<EventDetailsKey, String> actualEventDetails = actualHubEvent.getDetails();

        assertThat(actualEventDetails.get(session_event_type)).isEqualTo(FRAUD_DETECTED);
        assertThat(actualHubEvent.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(actualHubEvent.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(actualEventDetails.get(idp_entity_id)).isEqualTo(idpEntityId);
        assertThat(actualEventDetails.get(pid)).isEqualTo(persistentIdDto.getNameId());
        assertThat(actualEventDetails.get(idp_fraud_event_id)).isEqualTo(fraudEventId);
        assertThat(actualEventDetails.get(gpg45_status)).isEqualTo(fraudIndicator);
        assertThat(actualEventDetails.get(principal_ip_address_as_seen_by_idp)).isEqualTo(principalIpAddressSeenByIdp);
        assertThat(actualEventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddressSeenByHub);
    }

    @Test
    public void logSessionTimeoutEvent_shouldSendEvent() throws Exception {
        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        SessionId sessionId = aSessionId().build();
        DateTime sessionExpiryTimestamp = DateTime.now().minusMinutes(10);
        String transactionEntityId = "Some entity id";

        eventLogger.logSessionTimeoutEvent(sessionId, sessionExpiryTimestamp, transactionEntityId, null);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());

        EventSinkHubEvent actualEvent = eventCaptor.getValue();
        assertThat(actualEvent.getEventType()).isEqualTo(SESSION_EVENT);
        assertThat(actualEvent.getDetails().containsKey(session_event_type)).as("Message details does not contain an entry for Session Event Type.").isTrue();
        assertThat(actualEvent.getDetails().get(session_event_type)).isEqualTo(SESSION_TIMEOUT);

        String actualExpiryTimeString = actualEvent.getDetails().get(session_expiry_time);
        DateTime actualExpiryTime = DateTime.parse(actualExpiryTimeString);
        assertThat(actualExpiryTime.isEqual(sessionExpiryTimestamp)).as(MessageFormat.format("Expected timestamp {0} but got {1}", sessionExpiryTimestamp, actualExpiryTime)).isTrue();

        assertThat(actualEvent.getDetails().containsKey(transaction_entity_id)).as("Message details does not contain an entry for Transaction Entity Id").isEqualTo(actualEvent.getDetails().containsKey(transaction_entity_id));
        assertThat(actualEvent.getDetails().get(transaction_entity_id)).isEqualTo(transactionEntityId);
    }

    @Test
    public void logIdpSelectedEvent_shouldLogEventWithIdpSelected() {
        DateTimeFreezer.freezeTime();

        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        final String principalIpAddress = "some-principal-ip-address";
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().withLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2)).build();
        eventLogger.logIdpSelectedEvent(state, principalIpAddress);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());
        EventSinkHubEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(SESSION_EVENT);
        assertThat(event.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(event.getSessionId()).isEqualTo(state.getSessionId().getSessionId());
        assertThat(event.getOriginatingService()).isEqualTo(serviceInfo.getName());
        String actualExpiryTimeString = event.getDetails().get(session_expiry_time);
        DateTime eventSessionExpiryTime = DateTime.parse(actualExpiryTimeString);
        assertThat(eventSessionExpiryTime.isEqual(state.getSessionExpiryTimestamp())).as(MessageFormat.format("Expected timestamp {0} but got {1}", state.getSessionExpiryTimestamp(), eventSessionExpiryTime)).isTrue();
        Map<EventDetailsKey, String> eventDetails = event.getDetails();
        assertThat(eventDetails.get(session_event_type)).isEqualTo(IDP_SELECTED);
        assertThat(eventDetails.get(idp_entity_id)).isEqualTo(state.getIdpEntityId());
        assertThat(eventDetails.get(transaction_entity_id)).isEqualTo(state.getRequestIssuerEntityId());
        assertThat(eventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddress);
        assertThat(eventDetails.get(minimum_level_of_assurance)).isEqualTo(LevelOfAssurance.LEVEL_1.name());
        assertThat(eventDetails.get(required_level_of_assurance)).isEqualTo(LevelOfAssurance.LEVEL_2.name());
    }

    @Test
    public void logIdpRequesterErrorEvent_shouldLogEvent() {
        DateTimeFreezer.freezeTime();


        final String requestId = "request-id";
        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        final SessionId sessionId = aSessionId().build();
        final String transactionEntityId = "some-transaction-entity-id";
        final String errorMessage = "some error message";
        final String principalIpAddressSeenByHub = "some-principal-ip-address";
        final DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(30);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        eventLogger.logIdpRequesterErrorEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, Optional.fromNullable(errorMessage), principalIpAddressSeenByHub);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());
        EventSinkHubEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(SESSION_EVENT);
        assertThat(event.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(event.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(event.getOriginatingService()).isEqualTo(serviceInfo.getName());
        String actualExpiryTimeString = event.getDetails().get(session_expiry_time);
        DateTime eventSessionExpiryTime = DateTime.parse(actualExpiryTimeString);
        assertThat(eventSessionExpiryTime.isEqual(sessionExpiryTimestamp)).as(MessageFormat.format("Expected timestamp {0} but got {1}", sessionExpiryTimestamp, eventSessionExpiryTime)).isTrue();
        Map<EventDetailsKey, String> eventDetails = event.getDetails();
        assertThat(eventDetails.get(session_event_type)).isEqualTo(REQUESTER_ERROR);
        assertThat(eventDetails.get(transaction_entity_id)).isEqualTo(transactionEntityId);
        assertThat(eventDetails.get(message)).isEqualTo(errorMessage);
        assertThat(eventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddressSeenByHub);
    }

    @Test
    public void logIdpAuthnFailedEvent_shouldLogEvent() {
        DateTimeFreezer.freezeTime();

        final String requestId = "requestId";
        ServiceInfoConfiguration serviceInfo = ServiceInfoConfigurationBuilder.aServiceInfo().withName("some-originating-service-name").build();

        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        final SessionId sessionId = aSessionId().build();
        final String transactionEntityId = "some-transaction-entity-id";
        final String principalIpAddressSeenByHub = "some-principal-ip-address";
        final DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(30);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        eventLogger.logIdpAuthnFailedEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, principalIpAddressSeenByHub);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());
        EventSinkHubEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(SESSION_EVENT);
        assertThat(event.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(event.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(event.getOriginatingService()).isEqualTo(serviceInfo.getName());
        String actualExpiryTimeString = event.getDetails().get(session_expiry_time);
        DateTime eventSessionExpiryTime = DateTime.parse(actualExpiryTimeString);
        assertThat(eventSessionExpiryTime.isEqual(sessionExpiryTimestamp)).as(MessageFormat.format("Expected timestamp {0} but got {1}", sessionExpiryTimestamp, eventSessionExpiryTime)).isTrue();
        Map<EventDetailsKey, String> eventDetails = event.getDetails();
        assertThat(eventDetails.get(session_event_type)).isEqualTo(IDP_AUTHN_FAILED);
        assertThat(eventDetails.get(transaction_entity_id)).isEqualTo(transactionEntityId);
        assertThat(eventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddressSeenByHub);
        assertThat(eventDetails.get(request_id)).isEqualTo(requestId);
    }

    @Test
    public void logIdpAuthnPendingEvent_shouldLogEvent() {
        DateTimeFreezer.freezeTime();

        final String requestId = "requestId";
        ServiceInfoConfiguration serviceInfo = ServiceInfoConfigurationBuilder.aServiceInfo().withName("some-originating-service-name").build();

        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        final SessionId sessionId = aSessionId().build();
        final String transactionEntityId = "some-transaction-entity-id";
        final String principalIpAddressSeenByHub = "some-principal-ip-address";
        final DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(30);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        eventLogger.logPausedRegistrationEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, principalIpAddressSeenByHub);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());
        EventSinkHubEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(SESSION_EVENT);
        assertThat(event.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(event.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(event.getOriginatingService()).isEqualTo(serviceInfo.getName());
        String actualExpiryTimeString = event.getDetails().get(session_expiry_time);
        DateTime eventSessionExpiryTime = DateTime.parse(actualExpiryTimeString);
        assertThat(eventSessionExpiryTime.isEqual(sessionExpiryTimestamp)).as(MessageFormat.format("Expected timestamp {0} but got {1}", sessionExpiryTimestamp, eventSessionExpiryTime)).isTrue();
        Map<EventDetailsKey, String> eventDetails = event.getDetails();
        assertThat(eventDetails.get(session_event_type)).isEqualTo(IDP_AUTHN_PENDING);
        assertThat(eventDetails.get(transaction_entity_id)).isEqualTo(transactionEntityId);
        assertThat(eventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddressSeenByHub);
        assertThat(eventDetails.get(request_id)).isEqualTo(requestId);
    }


    @Test
    public void logIdpNoAuthnContext_shouldLogEvent() {
        DateTimeFreezer.freezeTime();

        final String requestId = "requestId";
        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        final SessionId sessionId = aSessionId().build();
        final String transactionEntityId = "some-transaction-entity-id";
        final String principalIpAddressSeenByHub = "some-principal-ip-address";
        final DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(30);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        eventLogger.logNoAuthnContextEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, principalIpAddressSeenByHub);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());
        EventSinkHubEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(SESSION_EVENT);
        assertThat(event.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(event.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(event.getOriginatingService()).isEqualTo(serviceInfo.getName());
        String actualExpiryTimeString = event.getDetails().get(session_expiry_time);
        DateTime eventSessionExpiryTime = DateTime.parse(actualExpiryTimeString);
        assertThat(eventSessionExpiryTime.isEqual(sessionExpiryTimestamp)).as(MessageFormat.format("Expected timestamp {0} but got {1}", sessionExpiryTimestamp, eventSessionExpiryTime)).isTrue();
        Map<EventDetailsKey, String> eventDetails = event.getDetails();
        assertThat(eventDetails.get(session_event_type)).isEqualTo(NO_AUTHN_CONTEXT);
        assertThat(eventDetails.get(transaction_entity_id)).isEqualTo(transactionEntityId);
        assertThat(eventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddressSeenByHub);
        assertThat(eventDetails.get(request_id)).isEqualTo(requestId);
    }

    @Test
    public void logCycle3DataObtained_shouldLogEvent() {
        DateTimeFreezer.freezeTime();

        final String requestId = "requestID";
        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        final SessionId sessionId = aSessionId().build();
        final String transactionEntityId = "some-transaction-entity-id";
        final String principalIpAddressSeenByHub = "some-principal-ip-address";
        final DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(30);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        eventLogger.logCycle3DataObtained(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId, principalIpAddressSeenByHub);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());
        EventSinkHubEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(SESSION_EVENT);
        assertThat(event.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(event.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(event.getOriginatingService()).isEqualTo(serviceInfo.getName());
        String actualExpiryTimeString = event.getDetails().get(session_expiry_time);
        DateTime eventSessionExpiryTime = DateTime.parse(actualExpiryTimeString);
        assertThat(eventSessionExpiryTime.isEqual(sessionExpiryTimestamp)).as(MessageFormat.format("Expected timestamp {0} but got {1}", sessionExpiryTimestamp, eventSessionExpiryTime)).isTrue();
        Map<EventDetailsKey, String> eventDetails = event.getDetails();
        assertThat(eventDetails.get(session_event_type)).isEqualTo(CYCLE3_DATA_OBTAINED);
        assertThat(eventDetails.get(transaction_entity_id)).isEqualTo(transactionEntityId);
        assertThat(eventDetails.get(principal_ip_address_as_seen_by_hub)).isEqualTo(principalIpAddressSeenByHub);
        assertThat(eventDetails.get(request_id)).isEqualTo(requestId);
    }

    @Test
    public void logMatchingServiceUserAccountCreationRequestSentEvent_shouldLogEvent() {
        DateTimeFreezer.freezeTime();

        final String requestId = "requestID";
        EventSinkHubEventLogger eventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy);
        final SessionId sessionId = aSessionId().build();
        final String transactionEntityId = "some-transaction-entity-id";
        final DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(30);
        ArgumentCaptor<EventSinkHubEvent> eventCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        eventLogger.logMatchingServiceUserAccountCreationRequestSentEvent(sessionId, transactionEntityId, sessionExpiryTimestamp, requestId);

        verify(eventSinkProxy).logHubEvent(eventCaptor.capture());
        EventSinkHubEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo(SESSION_EVENT);
        assertThat(event.getTimestamp()).isEqualTo(DateTime.now());
        assertThat(event.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(event.getOriginatingService()).isEqualTo(serviceInfo.getName());
        String actualExpiryTimeString = event.getDetails().get(session_expiry_time);
        DateTime eventSessionExpiryTime = DateTime.parse(actualExpiryTimeString);
        assertThat(eventSessionExpiryTime.isEqual(sessionExpiryTimestamp)).as(MessageFormat.format("Expected timestamp {0} but got {1}", sessionExpiryTimestamp, eventSessionExpiryTime)).isTrue();
        Map<EventDetailsKey, String> eventDetails = event.getDetails();
        assertThat(eventDetails.get(session_event_type)).isEqualTo(USER_ACCOUNT_CREATION_REQUEST_SENT);
        assertThat(eventDetails.get(transaction_entity_id)).isEqualTo(transactionEntityId);
        assertThat(eventDetails.get(request_id)).isEqualTo(requestId);
    }
}
