package uk.gov.ida.hub.policy.logging;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.ServiceInfoConfigurationBuilder;
import uk.gov.ida.eventemitter.EventDetailsKey;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.eventemitter.EventDetailsKey.analytics_session_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.downstream_uri;
import static uk.gov.ida.eventemitter.EventDetailsKey.error_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.gpg45_status;
import static uk.gov.ida.eventemitter.EventDetailsKey.hub_event_type;
import static uk.gov.ida.eventemitter.EventDetailsKey.idp_entity_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.idp_fraud_event_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.journey_type;
import static uk.gov.ida.eventemitter.EventDetailsKey.message;
import static uk.gov.ida.eventemitter.EventDetailsKey.message_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.minimum_level_of_assurance;
import static uk.gov.ida.eventemitter.EventDetailsKey.pid;
import static uk.gov.ida.eventemitter.EventDetailsKey.principal_ip_address_as_seen_by_hub;
import static uk.gov.ida.eventemitter.EventDetailsKey.principal_ip_address_as_seen_by_idp;
import static uk.gov.ida.eventemitter.EventDetailsKey.provided_level_of_assurance;
import static uk.gov.ida.eventemitter.EventDetailsKey.request_id;
import static uk.gov.ida.eventemitter.EventDetailsKey.required_level_of_assurance;
import static uk.gov.ida.eventemitter.EventDetailsKey.session_event_type;
import static uk.gov.ida.eventemitter.EventDetailsKey.session_expiry_time;
import static uk.gov.ida.eventemitter.EventDetailsKey.transaction_entity_id;
import static uk.gov.ida.hub.policy.builder.domain.FraudDetectedDetailsBuilder.aFraudDetectedDetails;
import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.ERROR_EVENT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.HUB_EVENT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.EventTypes.SESSION_EVENT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.HubEvents.RECEIVED_AUTHN_REQUEST_FROM_HUB;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_DATA_OBTAINED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.FRAUD_DETECTED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_FAILED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_PENDING;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_AUTHN_SUCCEEDED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.IDP_SELECTED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.NO_AUTHN_CONTEXT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.REQUESTER_ERROR;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.SESSION_STARTED;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.SESSION_TIMEOUT;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATION_REQUEST_SENT;

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
    private static final UUID ERROR_ID = UUID.randomUUID();
    private static final String ERROR_MESSAGE = "SAML error";
    private static final String ANALYTICS_SESSION_ID = "some-analytics-session-id";
    private static final String JOURNEY_TYPE = "some-journey-type";

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

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                message_id, samlResponse.getId(),
                minimum_level_of_assurance, MINIMUM_LEVEL_OF_ASSURANCE.name(),
                required_level_of_assurance, REQUIRED_LEVEL_OF_ASSURANCE.name(),
                session_event_type, SESSION_STARTED));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logEventSinkHubEvent_shouldSendEvent() {
        eventLogger.logRequestFromHub(SESSION_ID, TRANSACTION_ENTITY_ID);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(SERVICE_INFO, SESSION_ID, HUB_EVENT, Map.of(
                hub_event_type, RECEIVED_AUTHN_REQUEST_FROM_HUB,
                transaction_entity_id, TRANSACTION_ENTITY_ID));

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
            Optional.ofNullable(PRINCIPAL_IP_ADDRESS_SEEN_BY_IDP),
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
            ANALYTICS_SESSION_ID,
            JOURNEY_TYPE
        );

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                idp_entity_id, IDP_ENTITY_ID,
                pid, PERSISTENT_ID.getNameId(),
                minimum_level_of_assurance, MINIMUM_LEVEL_OF_ASSURANCE.name(),
                required_level_of_assurance, REQUIRED_LEVEL_OF_ASSURANCE.name(),
                provided_level_of_assurance, PROVIDED_LEVEL_OF_ASSURANCE.name(),
                principal_ip_address_as_seen_by_idp, PRINCIPAL_IP_ADDRESS_SEEN_BY_IDP,
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                session_event_type, IDP_AUTHN_SUCCEEDED,
                analytics_session_id, ANALYTICS_SESSION_ID,
                journey_type, JOURNEY_TYPE));

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
            REQUEST_ID,
            ANALYTICS_SESSION_ID,
            JOURNEY_TYPE
        );

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                session_event_type, FRAUD_DETECTED,
                idp_entity_id, IDP_ENTITY_ID,
                pid, PERSISTENT_ID.getNameId(),
                idp_fraud_event_id, fraudEventId,
                gpg45_status, fraudIndicator,
                principal_ip_address_as_seen_by_idp, PRINCIPAL_IP_ADDRESS_SEEN_BY_IDP,
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                analytics_session_id, ANALYTICS_SESSION_ID,
                journey_type, JOURNEY_TYPE));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logSessionTimeoutEvent_shouldSendEvent() {
        eventLogger.logSessionTimeoutEvent(SESSION_ID, SESSION_EXPIRY_TIMESTAMP, TRANSACTION_ENTITY_ID, REQUEST_ID);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(
                Map.of(session_event_type, SESSION_TIMEOUT)
        );

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

        eventLogger.logIdpSelectedEvent(state, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB, ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                session_event_type, IDP_SELECTED,
                idp_entity_id, IDP_ENTITY_ID,
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                minimum_level_of_assurance, MINIMUM_LEVEL_OF_ASSURANCE.name(),
                required_level_of_assurance, REQUIRED_LEVEL_OF_ASSURANCE.name(),
                analytics_session_id, ANALYTICS_SESSION_ID,
                journey_type, JOURNEY_TYPE));

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
            Optional.ofNullable(errorMessage),
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
            ANALYTICS_SESSION_ID,
            JOURNEY_TYPE
        );

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                message, errorMessage,
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                session_event_type, REQUESTER_ERROR,
                analytics_session_id, ANALYTICS_SESSION_ID,
                journey_type, JOURNEY_TYPE));

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
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
            ANALYTICS_SESSION_ID,
            JOURNEY_TYPE
        );

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                session_event_type, IDP_AUTHN_FAILED,
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                analytics_session_id, ANALYTICS_SESSION_ID,
                journey_type, JOURNEY_TYPE));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logIdpAuthnPendingEvent_shouldLogEvent() {
        eventLogger.logPausedRegistrationEvent(
            SESSION_ID,
            TRANSACTION_ENTITY_ID,
            SESSION_EXPIRY_TIMESTAMP,
            REQUEST_ID,
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
            ANALYTICS_SESSION_ID,
            JOURNEY_TYPE
        );

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                session_event_type, IDP_AUTHN_PENDING,
                analytics_session_id, ANALYTICS_SESSION_ID,
                journey_type, JOURNEY_TYPE));

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
            PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
            ANALYTICS_SESSION_ID,
            JOURNEY_TYPE
        );


        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                session_event_type, NO_AUTHN_CONTEXT,
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                analytics_session_id, ANALYTICS_SESSION_ID,
                journey_type, JOURNEY_TYPE));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void logCycle3DataObtained_shouldLogEvent() {
        eventLogger.logCycle3DataObtained(SESSION_ID, TRANSACTION_ENTITY_ID, SESSION_EXPIRY_TIMESTAMP, REQUEST_ID, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB);

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                principal_ip_address_as_seen_by_hub, PRINCIPAL_IP_ADDRESS_SEEN_BY_HUB,
                session_event_type, CYCLE3_DATA_OBTAINED));

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

        final EventSinkHubEvent expectedEvent = createExpectedEventSinkHubEvent(Map.of(
                session_event_type, USER_ACCOUNT_CREATION_REQUEST_SENT));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void shouldLogErrorEvent() {
        eventLogger.logErrorEvent(ERROR_ID, SESSION_ID, ERROR_MESSAGE);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            ERROR_EVENT,
            Map.of(
                    message, ERROR_MESSAGE,
                    error_id, ERROR_ID.toString()
            ));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void shouldLogErrorEventContainingIDPEntityId() {
        final String idpEntityId = "IDP entity id";

        eventLogger.logErrorEvent(ERROR_ID, idpEntityId, SESSION_ID);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            ERROR_EVENT,
            Map.of(
                    idp_entity_id, idpEntityId,
                    error_id, ERROR_ID.toString()
            ));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void shouldLogErrorEventContainingDownstreamUri() {
        final String downstreamUri = "uri";

        eventLogger.logErrorEvent(ERROR_ID, SESSION_ID, ERROR_MESSAGE, downstreamUri);

        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            ERROR_EVENT,
            Map.of(
                    downstream_uri, downstreamUri,
                    message, ERROR_MESSAGE,
                    error_id, ERROR_ID.toString()
            ));

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @Test
    public void shouldLogErrorEventContainingAnErrorMessage() {
        eventLogger.logErrorEvent(ERROR_MESSAGE, SESSION_ID);


        final EventSinkHubEvent expectedEvent = new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            ERROR_EVENT,
            Map.of(message, ERROR_MESSAGE)
        );

        verify(eventSinkProxy).logHubEvent(argThat(new EventMatching(expectedEvent)));
        verify(eventEmitter).record(argThat(new EventMatching(expectedEvent)));
    }

    @NotNull
    private EventSinkHubEvent createExpectedEventSinkHubEvent(Map<EventDetailsKey, String> extraDetails) {
        Map<EventDetailsKey, String> details = new HashMap<>(Map.of(
                session_expiry_time, SESSION_EXPIRY_TIMESTAMP.toString(),
                transaction_entity_id, TRANSACTION_ENTITY_ID,
                request_id, REQUEST_ID));

        details.putAll(extraDetails);

        return new EventSinkHubEvent(
            SERVICE_INFO,
            SESSION_ID,
            SESSION_EVENT,
            Map.copyOf(details)
        );
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
