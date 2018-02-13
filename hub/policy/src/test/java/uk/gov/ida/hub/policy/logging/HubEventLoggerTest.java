package uk.gov.ida.hub.policy.logging;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.ServiceInfoConfigurationBuilder;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
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
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_DATA_OBTAINED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.FRAUD_DETECTED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_FAILED;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_PENDING;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_SUCCEEDED;
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
public class HubEventLoggerTest {

    private static final PersistentId PERSISTENT_ID = aPersistentId().withNameId("nameId").build();
    private static final String TRANSACTION_ENTITY_ID = "transaction-entity-id";
    private static final String IDP_ENTITY_ID = "idp entity id";
    private static final LevelOfAssurance MINIMUM_LEVEL_OF_ASSURANCE = LevelOfAssurance.LEVEL_1;
    private static final LevelOfAssurance REQUIRED_LEVEL_OF_ASSURANCE = LevelOfAssurance.LEVEL_2;
    private static final LevelOfAssurance PROVIDED_LEVEL_OF_ASSURANCE = LevelOfAssurance.LEVEL_2;
    private static final String REQUEST_ID = "requestId";
    private static final SessionId SESSION_ID = aSessionId().build();
    private static final String PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB = "some-ip-address";
    private static final String PRINCIPAL_IP_ADDRESS_SEEN_BY_IDP = "principal-ip-address-seen-by-idp";
    private static final ServiceInfoConfiguration SERVICE_INFO = ServiceInfoConfigurationBuilder.aServiceInfo().withName("service-name").build();
    private static final DateTime SESSION_EXPIRY_TIMESTAMP = DateTime.now().minusMinutes(10);

    @Mock
    private EventSinkProxy eventSinkProxy;

    @Mock
    private EventEmitter eventEmitter;

    private HubEventLogger eventLogger;

    @Before
    public void setUp() {
        DateTimeFreezer.freezeTime();
        eventLogger = new HubEventLogger(SERVICE_INFO, eventSinkProxy, eventEmitter);
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void logSessionOpenEvent_shouldSendEvent() {
        final SamlResponseWithAuthnRequestInformationDto samlResponse = aSamlResponseWithAuthnRequestInformationDto()
                .withId(REQUEST_ID)
                .withIssuer(TRANSACTION_ENTITY_ID)
                .build();

        eventLogger.logSessionStartedEvent(samlResponse, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB, SESSION_EXPIRY_TIMESTAMP, SESSION_ID, MINIMUM_LEVEL_OF_ASSURANCE, REQUIRED_LEVEL_OF_ASSURANCE);

        final Map<EventDetailsKey, String> details = new HashMap<>();
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);
        details.put(message_id, samlResponse.getId());
        details.put(minimum_level_of_assurance, MINIMUM_LEVEL_OF_ASSURANCE.name());
        details.put(required_level_of_assurance, REQUIRED_LEVEL_OF_ASSURANCE.name());
        details.put(session_event_type, SESSION_STARTED);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logEventSinkHubEvent_shouldSendEvent() {
        eventLogger.logRequestFromHub(SESSION_ID, TRANSACTION_ENTITY_ID);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(hub_event_type, RECEIVED_AUTHN_REQUEST_FROM_HUB);
        details.put(transaction_entity_id, TRANSACTION_ENTITY_ID);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(SERVICE_INFO, SESSION_ID, HUB_EVENT, details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logIdpAuthnSucceededEvent_shouldContainLevelsOfAssuranceInDetailsFieldForBilling() {
        eventLogger.logIdpAuthnSucceededEvent(
            SESSION_ID,
            SESSION_EXPIRY_TIMESTAMP,
            IDP_ENTITY_ID,
            TRANSACTION_ENTITY_ID,
            PERSISTENT_ID,
            REQUEST_ID,
            MINIMUM_LEVEL_OF_ASSURANCE,
            REQUIRED_LEVEL_OF_ASSURANCE,
            PROVIDED_LEVEL_OF_ASSURANCE,
            Optional.fromNullable(PRINCIPAL_IP_ADDRESS_SEEN_BY_IDP),
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(idp_entity_id, IDP_ENTITY_ID);
        details.put(pid, PERSISTENT_ID.getNameId());
        details.put(minimum_level_of_assurance, MINIMUM_LEVEL_OF_ASSURANCE.name());
        details.put(required_level_of_assurance, REQUIRED_LEVEL_OF_ASSURANCE.name());
        details.put(provided_level_of_assurance, PROVIDED_LEVEL_OF_ASSURANCE.name());
        details.put(principal_ip_address_as_seen_by_idp, PRINCIPAL_IP_ADDRESS_SEEN_BY_IDP);
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);
        details.put(session_event_type, IDP_AUTHN_SUCCEEDED);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logIdpFraudEvent_shouldLogFraudEventWithDetails() {
        final String fraudEventId = "fraudEventId";
        final String fraudIndicator = "FI02";
        final FraudDetectedDetails fraudDetectedDetailsDto = aFraudDetectedDetails()
            .withFraudEventId(fraudEventId)
            .withFraudIndicator(fraudIndicator)
            .build();

        eventLogger.logIdpFraudEvent(
            SESSION_ID,
            IDP_ENTITY_ID,
            TRANSACTION_ENTITY_ID,
            PERSISTENT_ID,
            SESSION_EXPIRY_TIMESTAMP,
            fraudDetectedDetailsDto,
            Optional.of(PRINCIPAL_IP_ADDRESS_SEEN_BY_IDP),
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
            REQUEST_ID
        );

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(session_event_type, FRAUD_DETECTED);
        details.put(idp_entity_id, IDP_ENTITY_ID);
        details.put(pid, PERSISTENT_ID.getNameId());
        details.put(idp_fraud_event_id, fraudEventId);
        details.put(gpg45_status, fraudIndicator);
        details.put(principal_ip_address_as_seen_by_idp, PRINCIPAL_IP_ADDRESS_SEEN_BY_IDP);
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logSessionTimeoutEvent_shouldSendEvent() {
        eventLogger.logSessionTimeoutEvent(SESSION_ID, SESSION_EXPIRY_TIMESTAMP, TRANSACTION_ENTITY_ID, REQUEST_ID);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(session_event_type, SESSION_TIMEOUT);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logIdpSelectedEvent_shouldLogEventWithIdpSelected() {
        final IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState()
            .withLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withSessionExpiryTimestamp(SESSION_EXPIRY_TIMESTAMP)
            .withIdpEntityId(IDP_ENTITY_ID)
            .withRequestIssuerEntityId(TRANSACTION_ENTITY_ID)
            .withRequestId(REQUEST_ID)
            .withSessionId(SESSION_ID)
            .build();

        eventLogger.logIdpSelectedEvent(state, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(session_event_type, IDP_SELECTED);
        details.put(idp_entity_id, IDP_ENTITY_ID);
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);
        details.put(minimum_level_of_assurance, MINIMUM_LEVEL_OF_ASSURANCE.name());
        details.put(required_level_of_assurance, REQUIRED_LEVEL_OF_ASSURANCE.name());

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logIdpRequesterErrorEvent_shouldLogEvent() {
        final String errorMessage = "some error message";

        eventLogger.logIdpRequesterErrorEvent(
            SESSION_ID,
            TRANSACTION_ENTITY_ID,
            SESSION_EXPIRY_TIMESTAMP,
            REQUEST_ID,
            Optional.fromNullable(errorMessage),
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(message, errorMessage);
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);
        details.put(session_event_type, REQUESTER_ERROR);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logIdpAuthnFailedEvent_shouldLogEvent() {

        eventLogger.logIdpAuthnFailedEvent(
            SESSION_ID,
            TRANSACTION_ENTITY_ID,
            SESSION_EXPIRY_TIMESTAMP,
            REQUEST_ID,
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB
        );

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(session_event_type, IDP_AUTHN_FAILED);
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logIdpAuthnPendingEvent_shouldLogEvent() {
        eventLogger.logPausedRegistrationEvent(SESSION_ID, TRANSACTION_ENTITY_ID, SESSION_EXPIRY_TIMESTAMP, REQUEST_ID, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);
        details.put(session_event_type, IDP_AUTHN_PENDING);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logIdpNoAuthnContext_shouldLogEvent() {
        eventLogger.logNoAuthnContextEvent(
            SESSION_ID,
            TRANSACTION_ENTITY_ID,
            SESSION_EXPIRY_TIMESTAMP,
            REQUEST_ID,
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB
        );

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(session_event_type, NO_AUTHN_CONTEXT);
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logCycle3DataObtained_shouldLogEvent() {
        eventLogger.logCycle3DataObtained(SESSION_ID, TRANSACTION_ENTITY_ID, SESSION_EXPIRY_TIMESTAMP, REQUEST_ID, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);
        details.put(session_event_type, CYCLE3_DATA_OBTAINED);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logMatchingServiceUserAccountCreationRequestSentEvent_shouldLogEvent() {
        eventLogger.logMatchingServiceUserAccountCreationRequestSentEvent(
            SESSION_ID,
            TRANSACTION_ENTITY_ID,
            SESSION_EXPIRY_TIMESTAMP,
            REQUEST_ID
        );

        final Map<EventDetailsKey, String> details = Maps.newHashMap();
        details.put(session_event_type, USER_ACCOUNT_CREATION_REQUEST_SENT);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(details);

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @NotNull
    private EventSinkHubEvent createExpectedEventSinkHubEvent(Map<EventDetailsKey, String> details) {
        details.put(session_expiry_time, SESSION_EXPIRY_TIMESTAMP.toString());
        details.put(transaction_entity_id, TRANSACTION_ENTITY_ID);
        details.put(request_id, REQUEST_ID);

        return new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            SESSION_EVENT,
            details
        );
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
