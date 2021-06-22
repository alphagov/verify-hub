package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.ServiceInfoConfigurationBuilder;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.eventemitter.EventDetailsKey;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.NoMatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;
import uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.policy.builder.domain.MatchFromMatchingServiceBuilder.aMatchFromMatchingService;
import static uk.gov.ida.hub.policy.builder.state.Cycle3MatchRequestSentStateBuilder.aCycle3MatchRequestSentState;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_MATCH;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_NO_MATCH;
import static uk.gov.ida.hub.shared.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATION_REQUEST_SENT;

@RunWith(MockitoJUnitRunner.class)
public class Cycle3MatchRequestSentStateControllerTest {
    @Mock
    private StateTransitionAction stateTransitionAction;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    @Mock
    private PolicyConfiguration policyConfiguration;

    @Mock
    private AssertionRestrictionsFactory assertionRestrictionFactory;

    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    @Mock
    private AttributeQueryService attributeQueryService;

    @Mock
    private EventSinkProxy eventSinkProxy;

    @Mock
    private EventEmitter eventEmitter;

    private HubEventLogger hubEventLogger;

    private final ServiceInfoConfiguration serviceInfo = ServiceInfoConfigurationBuilder.aServiceInfo().build();
    private final String matchingServiceEntityId = "matchingServiceEntityId";
    private final ResponseFromHubFactory responseFromHubFactory = new ResponseFromHubFactory(new IdGenerator());

    @Captor
    ArgumentCaptor<AttributeQueryRequestDto> attributeQueryRequestCaptor = null;

    @Before
    public void setUp() {
        hubEventLogger = new HubEventLogger(serviceInfo, eventSinkProxy, eventEmitter);
    }

    @Test
    public void shouldThrowStateProcessingValidationExceptionIfResponseIsNotFromTheExpectedMatchingService() {
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, null, null, null, null, null, transactionsConfigProxy, null, null, null);

        MatchFromMatchingService matchFromMatchingService = aMatchFromMatchingService().withIssuerId("issuer-id").build();

        try {
            controller.validateResponse(matchFromMatchingService);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo(format("Response to request ID [{0}] came from [issuer-id] and was expected to come from [{1}]", state.getRequestId(), matchingServiceEntityId));
        }
    }

    @Test
    public void shouldReturnUserAccountCreationRequestSentStateForNoMatchWhenAttributesArePresent() {
        ArgumentCaptor<UserAccountCreationRequestSentState> capturedState = ArgumentCaptor.forClass(UserAccountCreationRequestSentState.class);
        ArgumentCaptor<EventSinkHubEvent> eventSinkArgumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        URI userAccountCreationUri = URI.create("a-test-user-account-creation-uri");
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().build();
        List<UserAccountCreationAttribute> userAccountCreationAttributes = List.of(UserAccountCreationAttribute.DATE_OF_BIRTH);
        String transactionEntityId = "request issuer id";
        when(transactionsConfigProxy.getUserAccountCreationAttributes(transactionEntityId)).thenReturn(userAccountCreationAttributes);
        when(matchingServiceConfigProxy.getMatchingService(anyString()))
                .thenReturn(aMatchingServiceConfigEntityDataDto().withUserAccountCreationUri(userAccountCreationUri).build());

        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, null, null,
                        transactionsConfigProxy, matchingServiceConfigProxy, assertionRestrictionFactory, attributeQueryService);

        controller.transitionToNextStateForNoMatchResponse();

        verify(eventSinkProxy, times(1)).logHubEvent(eventSinkArgumentCaptor.capture());
        assertThat(eventSinkArgumentCaptor.getValue().getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.SESSION_EVENT);
        assertThat(eventSinkArgumentCaptor.getValue().getDetails().get(EventDetailsKey.session_event_type)).isEqualTo(USER_ACCOUNT_CREATION_REQUEST_SENT);
        assertThat(eventSinkArgumentCaptor.getValue().getSessionId()).isEqualTo(state.getSessionId().toString());
        assertThat(eventSinkArgumentCaptor.getValue().getDetails().get(EventDetailsKey.request_id)).isEqualTo(state.getRequestId());
        assertThat(eventSinkArgumentCaptor.getValue().getOriginatingService()).isEqualTo(serviceInfo.getName());

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        verify(attributeQueryService).sendAttributeQueryRequest(eq(capturedState.getValue().getSessionId()), attributeQueryRequestCaptor.capture());

        AttributeQueryRequestDto actualAttributeQueryRequestDto = attributeQueryRequestCaptor.getValue();
        assertThat(actualAttributeQueryRequestDto.getAttributeQueryUri()).isEqualTo(userAccountCreationUri);
        assertThat(actualAttributeQueryRequestDto.getUserAccountCreationAttributes()).isEqualTo(Optional.ofNullable(userAccountCreationAttributes));
        assertThat(actualAttributeQueryRequestDto.getEncryptedMatchingDatasetAssertion()).isEqualTo(state.getEncryptedMatchingDatasetAssertion());

        assertThat(capturedState.getValue()).isInstanceOf(UserAccountCreationRequestSentState.class);
    }

    @Test
    public void shouldTransitonToNoMatchStateForNoMatchResponseWhenNoAttributesArePresent(){
        ArgumentCaptor<NoMatchState> capturedState = ArgumentCaptor.forClass(NoMatchState.class);
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().build();
        List<UserAccountCreationAttribute> userAccountCreationAttributes = emptyList();
        when(transactionsConfigProxy.getUserAccountCreationAttributes("request issuer id")).thenReturn(userAccountCreationAttributes);
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, null, null, null, transactionsConfigProxy, null, null, null);

        controller.transitionToNextStateForNoMatchResponse();

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isInstanceOf(NoMatchState.class);
    }

    @Test
    public void shouldLogCycle3NoMatchEventToEventSinkForCycle3NoMatchResponseFromMatchingService() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, null, null,
                        transactionsConfigProxy, matchingServiceConfigProxy, assertionRestrictionFactory, attributeQueryService);
        ArgumentCaptor<EventSinkHubEvent> argumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(matchingServiceEntityId, requestId);
        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(eventSinkProxy, times(1)).logHubEvent(argumentCaptor.capture());
        final EventSinkHubEvent eventSinkHubEvent = argumentCaptor.getValue();
        assertThat(eventSinkHubEvent.getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.SESSION_EVENT);
        assertThat(eventSinkHubEvent.getDetails().get(EventDetailsKey.session_event_type)).isEqualTo(CYCLE3_NO_MATCH);
        assertThat(eventSinkHubEvent.getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(eventSinkHubEvent.getDetails().get(EventDetailsKey.request_id)).isEqualTo(requestId);
        assertThat(eventSinkHubEvent.getOriginatingService()).isEqualTo(serviceInfo.getName());
    }

    @Test
    public void shouldLogCycle3MatchEventToEventSinkForCycle3SuccessfulMatchResponseFromMatchingService() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, mock(StateTransitionAction.class),
                        policyConfiguration, mock(LevelOfAssuranceValidator.class), null, transactionsConfigProxy, matchingServiceConfigProxy,
                        assertionRestrictionFactory, attributeQueryService);
        ArgumentCaptor<EventSinkHubEvent> argumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        MatchFromMatchingService matchFromMatchingService = new MatchFromMatchingService(matchingServiceEntityId, requestId, "assertionBlob", Optional.of(LevelOfAssurance.LEVEL_1));
        controller.handleMatchResponseFromMatchingService(matchFromMatchingService);

        verify(eventSinkProxy, times(1)).logHubEvent(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.SESSION_EVENT);
        assertThat(argumentCaptor.getValue().getDetails().get(EventDetailsKey.session_event_type)).isEqualTo(CYCLE3_MATCH);
        assertThat(argumentCaptor.getValue().getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(argumentCaptor.getValue().getDetails().get(EventDetailsKey.request_id)).isEqualTo(requestId);
        assertThat(argumentCaptor.getValue().getOriginatingService()).isEqualTo(serviceInfo.getName());
    }

    @Test
    public void shouldNotLogCycle3MatchEventToEventSinkForCycle3NoMatchResponseFromMatchingService() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, mock(StateTransitionAction.class),
                        policyConfiguration, null, null, transactionsConfigProxy, matchingServiceConfigProxy,
                        assertionRestrictionFactory, attributeQueryService);
        ArgumentCaptor<EventSinkHubEvent> argumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(matchingServiceEntityId, requestId);
        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(eventSinkProxy, times(1)).logHubEvent(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getDetails().get(EventDetailsKey.session_event_type)).isNotEqualTo(CYCLE3_MATCH);
        assertThat(argumentCaptor.getValue().getDetails().get(EventDetailsKey.session_event_type)).isEqualTo(CYCLE3_NO_MATCH);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInAwaitingCycle3DataState() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, null, policyConfiguration, null, responseFromHubFactory,
                        transactionsConfigProxy, matchingServiceConfigProxy, assertionRestrictionFactory, attributeQueryService);

        final ResponseFromHub responseFromHub = controller.getErrorResponse();

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test(expected=StateProcessingValidationException.class)
    public void shouldThrowExceptionWhenInResponseToDoesNotMatchFromCycle3MatchRequestForMatchResponse() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, null, policyConfiguration,
                        mock(LevelOfAssuranceValidator.class), null, transactionsConfigProxy, matchingServiceConfigProxy,
                        assertionRestrictionFactory, attributeQueryService);

        MatchFromMatchingService matchFromMatchingService = new MatchFromMatchingService(matchingServiceEntityId, "definitelyNotTheSameRequestId", "assertionBlob", Optional.of(LevelOfAssurance.LEVEL_1));
        controller.handleMatchResponseFromMatchingService(matchFromMatchingService);

    }

    @Test
    public void statusShouldSendNoMatchResponseToTransactionWhenNoMatchResponseReceivedFromMatchingServiceCycle3Match() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration,
                        null, null, transactionsConfigProxy, matchingServiceConfigProxy, assertionRestrictionFactory, attributeQueryService);
        ArgumentCaptor<NoMatchState> argumentCaptor = ArgumentCaptor.forClass(NoMatchState.class);
        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(matchingServiceEntityId, requestId);

        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(stateTransitionAction, times(1)).transitionTo(argumentCaptor.capture());
        NoMatchStateController noMatchStateController = new NoMatchStateController(argumentCaptor.getValue(), responseFromHubFactory);
        ResponseProcessingDetails responseProcessingDetails = noMatchStateController.getResponseProcessingDetails();
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SEND_NO_MATCH_RESPONSE_TO_TRANSACTION);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    public void shouldMoveFromAwaitingC3StateToCycle3DataSentStateWhenCycle3DataIsReceived() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration,
                        null, null, transactionsConfigProxy, matchingServiceConfigProxy, assertionRestrictionFactory, attributeQueryService);
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(5));

        ResponseProcessingDetails responseProcessingDetails = controller.getResponseProcessingDetails();

        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.WAIT);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    public void shouldReturnWaitResponseWhenAskedAndInCycle3MatchRequestSentState() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration,
                        null, null, transactionsConfigProxy, matchingServiceConfigProxy, assertionRestrictionFactory, attributeQueryService);
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(5));

        final ResponseProcessingDetails responseProcessingDetails = controller.getResponseProcessingDetails();
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.WAIT);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInCycle3MatchRequestSentState() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        Cycle3MatchRequestSentState state = aCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        Cycle3MatchRequestSentStateController controller =
                new Cycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration,
                        null, responseFromHubFactory, transactionsConfigProxy, matchingServiceConfigProxy,
                        assertionRestrictionFactory, attributeQueryService);

        final ResponseFromHub responseFromHub = controller.getErrorResponse();
        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }
}
