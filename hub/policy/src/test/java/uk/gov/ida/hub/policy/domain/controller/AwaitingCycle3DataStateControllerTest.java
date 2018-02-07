package uk.gov.ida.hub.policy.domain.controller;

import org.assertj.core.api.Condition;
import org.assertj.core.condition.AllOf;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.EventSinkProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_CANCEL;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_DATA_OBTAINED;
import static uk.gov.ida.hub.policy.builder.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.builder.state.AwaitingCycle3DataStateBuilder.anAwaitingCycle3DataState;
import static uk.gov.ida.hub.policy.matchers.HasDetail.hasDetail;
import static uk.gov.ida.hub.policy.matchers.HasSessionId.hasSessionId;

@RunWith(MockitoJUnitRunner.class)
public class AwaitingCycle3DataStateControllerTest {

    @Mock
    private EventSinkProxy eventSinkProxy;
    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;
    @Mock
    private PolicyConfiguration policyConfiguration;
    @Mock
    private AssertionRestrictionsFactory assertionRestrictionsFactory;
    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;
    @Mock
    private EventEmitter eventEmitter;

    private ServiceInfoConfiguration serviceInfo = new ServiceInfoConfiguration("service-name");
    private EventSinkHubEventLogger eventSinkHubEventLogger;

    @Before
    public void setUp() {
        eventSinkHubEventLogger = new EventSinkHubEventLogger(serviceInfo, eventSinkProxy, eventEmitter);
    }

    @Test
    public void handleCycle3DataSubmitted_shouldLogCycle3DataObtainedAndPrincipalIpAddressSeenByHubToEventSink() {
        final SessionId sessionId = aSessionId().build();
        final String principalIpAddressAsSeenByHub = "principal-ip-address-as-seen-by-hub";
        final String requestId = "requestId";

        final AwaitingCycle3DataStateController awaitingCycle3DataStateController = setUpAwaitingCycle3DataStateController(requestId, sessionId);

        awaitingCycle3DataStateController.handleCycle3DataSubmitted(principalIpAddressAsSeenByHub);

        ArgumentCaptor<EventSinkHubEvent> argumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy, atLeastOnce()).logHubEvent(argumentCaptor.capture());

        Condition<EventSinkHubEvent> combinedConditions = AllOf.allOf(
                hasSessionId(sessionId),
                hasDetail(EventDetailsKey.session_event_type, CYCLE3_DATA_OBTAINED),
                hasDetail(EventDetailsKey.request_id, requestId),
                hasDetail(EventDetailsKey.principal_ip_address_as_seen_by_hub, principalIpAddressAsSeenByHub));
        assertThat(argumentCaptor.getAllValues()).haveAtLeast(1, combinedConditions);
    }

    @Test
    public void cycle3dataInputCancelledFromFrontEnd_shouldLogCancellation() throws Exception {
        final String requestId = "requestId";
        final SessionId sessionId = aSessionId().build();

        final AwaitingCycle3DataStateController awaitingCycle3DataStateController = setUpAwaitingCycle3DataStateController(requestId, sessionId);

        awaitingCycle3DataStateController.handleCancellation();

        ArgumentCaptor<EventSinkHubEvent> argumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy, atLeastOnce()).logHubEvent(argumentCaptor.capture());

        Condition<EventSinkHubEvent> combinedConditions = AllOf.allOf(
                hasSessionId(sessionId),
                hasDetail(EventDetailsKey.session_event_type, CYCLE3_CANCEL),
                hasDetail(EventDetailsKey.request_id, requestId));
        assertThat(argumentCaptor.getAllValues()).haveAtLeast(1, combinedConditions);

    }

    private AwaitingCycle3DataStateController setUpAwaitingCycle3DataStateController(String requestId, SessionId sessionId) {
        final String transactionEntityId = "some-transaction-entity-id";
        final DateTime sessionExpiryTime = DateTime.now().plusMinutes(1);

        AwaitingCycle3DataState state = anAwaitingCycle3DataState()
                .withSessionId(sessionId)
                .withTransactionEntityId(transactionEntityId)
                .withSessionExpiryTime(sessionExpiryTime)
                .withRequestId(requestId)
                .build();

        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(new Duration(600L));

        final AwaitingCycle3DataStateController awaitingCycle3DataStateController = new AwaitingCycle3DataStateController(
                state,
                eventSinkHubEventLogger,
                stateTransitionAction,
                transactionsConfigProxy,
                responseFromHubFactory,
                policyConfiguration,
                assertionRestrictionsFactory,
                matchingServiceConfigProxy);

        when(matchingServiceConfigProxy.getMatchingService(state.getMatchingServiceEntityId())).thenReturn(aMatchingServiceConfigEntityDataDto().build());
        return awaitingCycle3DataStateController;
    }

    @Test
    public void shouldMoveFromAwaitingC3StateToCycle3DataSentStateWhenCycle3DataIsReceived() throws Exception {
        final SessionId sessionId = SessionId.createNewSessionId();
        AwaitingCycle3DataState state = anAwaitingCycle3DataState().withSessionId(sessionId).build();
        AwaitingCycle3DataStateController controller = new AwaitingCycle3DataStateController(state, eventSinkHubEventLogger, stateTransitionAction, transactionsConfigProxy, responseFromHubFactory, policyConfiguration, assertionRestrictionsFactory, matchingServiceConfigProxy);
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(5));
        ArgumentCaptor<Cycle3MatchRequestSentStateTransitional> argumentCaptor = ArgumentCaptor.forClass(Cycle3MatchRequestSentStateTransitional.class);
        when(matchingServiceConfigProxy.getMatchingService(state.getMatchingServiceEntityId())).thenReturn(aMatchingServiceConfigEntityDataDto().build());

        controller.handleCycle3DataSubmitted("principalIpAsSeenByHub");

        verify(stateTransitionAction, times(1)).transitionTo(argumentCaptor.capture());
        final Cycle3MatchRequestSentStateTransitional cycle3MatchRequestSentState = argumentCaptor.getValue();
        assertThat(cycle3MatchRequestSentState.getEncryptedMatchingDatasetAssertion()).isEqualTo(state.getEncryptedMatchingDatasetAssertion());
    }
}
