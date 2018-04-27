package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.ServiceInfoConfigurationBuilder;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.NoMatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.net.URI;

import static java.text.MessageFormat.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_MATCH;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.CYCLE3_NO_MATCH;
import static uk.gov.ida.eventsink.EventSinkHubEventConstants.SessionEvents.USER_ACCOUNT_CREATION_REQUEST_SENT;
import static uk.gov.ida.hub.policy.builder.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.policy.builder.domain.MatchFromMatchingServiceBuilder.aMatchFromMatchingService;
import static uk.gov.ida.hub.policy.builder.state.EidasCycle3MatchRequestSentStateBuilder.anEidasCycle3MatchRequestSentState;

@RunWith(MockitoJUnitRunner.class)
public class EidasCycle3MatchRequestSentStateControllerTest {
    @Mock
    private StateTransitionAction stateTransitionAction;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    @Mock
    private PolicyConfiguration policyConfiguration;

    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    @Mock
    private AttributeQueryService attributeQueryService;

    @Mock
    private EventSinkProxy eventSinkProxy;

    @Mock
    private EventEmitter eventEmitter;

    @Mock
    private LevelOfAssuranceValidator levelOfAssuranceValidator;

    private HubEventLogger hubEventLogger;

    private final ServiceInfoConfiguration serviceInfo = ServiceInfoConfigurationBuilder.aServiceInfo().build();
    private final String matchingServiceEntityId = "matchingServiceAdapterEntityId";
    private final ResponseFromHubFactory responseFromHubFactory = new ResponseFromHubFactory(new IdGenerator());

    @Captor
    ArgumentCaptor<EidasAttributeQueryRequestDto> attributeQueryRequestCaptor = null;

    @Before
    public void setUp() {
        hubEventLogger = new HubEventLogger(serviceInfo, eventSinkProxy, eventEmitter);
    }

    @Test
    public void getNextState_shouldThrowStateProcessingValidationExceptionIfResponseIsNotFromTheExpectedMatchingService() {
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
        MatchFromMatchingService matchFromMatchingService = aMatchFromMatchingService().withIssuerId("issuer-id").build();

        try {
            controller.validateResponse(matchFromMatchingService);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo(format("Response to request ID [{0}] came from [issuer-id] and was expected to come from [{1}]", state.getRequestId(), matchingServiceEntityId));
        }
    }

    @Test
    public void getNextStateForNoMatch_shouldReturnUserAccountCreationRequestSentStateWhenAttributesArePresent() {
        //Given
        URI userAccountCreationUri = URI.create("a-test-user-account-creation-uri");
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().build();
        ImmutableList<UserAccountCreationAttribute> userAccountCreationAttributes = ImmutableList.of(UserAccountCreationAttribute.DATE_OF_BIRTH);
        when(transactionsConfigProxy.getUserAccountCreationAttributes(state.getRequestIssuerEntityId())).thenReturn(userAccountCreationAttributes);
        when(matchingServiceConfigProxy.getMatchingService(anyString()))
                .thenReturn(aMatchingServiceConfigEntityDataDto().withUserAccountCreationUri(userAccountCreationUri).build());

        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);

        //When
        State nextState = controller.getNextStateForNoMatch();

        //Then
        ArgumentCaptor<EventSinkHubEvent> eventSinkArgumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        verify(eventSinkProxy, times(1)).logHubEvent(eventSinkArgumentCaptor.capture());
        assertThat(eventSinkArgumentCaptor.getValue().getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.SESSION_EVENT);
        assertThat(eventSinkArgumentCaptor.getValue().getDetails().get(EventDetailsKey.session_event_type)).isEqualTo(USER_ACCOUNT_CREATION_REQUEST_SENT);
        assertThat(eventSinkArgumentCaptor.getValue().getSessionId()).isEqualTo(state.getSessionId().toString());
        assertThat(eventSinkArgumentCaptor.getValue().getDetails().get(EventDetailsKey.request_id)).isEqualTo(state.getRequestId());
        assertThat(eventSinkArgumentCaptor.getValue().getOriginatingService()).isEqualTo(serviceInfo.getName());

        verify(attributeQueryService).sendAttributeQueryRequest(eq(nextState.getSessionId()), attributeQueryRequestCaptor.capture());

        EidasAttributeQueryRequestDto actualAttributeQueryRequestDto = attributeQueryRequestCaptor.getValue();
        assertThat(actualAttributeQueryRequestDto.getAttributeQueryUri()).isEqualTo(userAccountCreationUri);
        assertThat(actualAttributeQueryRequestDto.getUserAccountCreationAttributes()).isEqualTo(Optional.fromNullable(userAccountCreationAttributes));
        assertThat(actualAttributeQueryRequestDto.getEncryptedIdentityAssertion()).isEqualTo(state.getEncryptedIdentityAssertion());

        assertThat(nextState).isInstanceOf(UserAccountCreationRequestSentState.class);
    }

    @Test
    public void getNextStateForNoMatch_shouldReturnNoMatchWhenNoAttributesArePresent(){
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().build();
        ImmutableList<UserAccountCreationAttribute> userAccountCreationAttributes = ImmutableList.of();
        when(transactionsConfigProxy.getUserAccountCreationAttributes(state.getRequestIssuerEntityId())).thenReturn(userAccountCreationAttributes);
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
        State nextState = controller.getNextStateForNoMatch();

        assertThat(nextState).isInstanceOf(NoMatchState.class);
    }

    @Test
    public void cycle3NoMatchResponseFromMatchingService_shouldLogCycle3NoMatchEventToEventSink() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
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
    public void cycle3SuccessfulMatchResponseFromMatchingService_shouldLogCycle3MatchEventToEventSink() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
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
    public void cycle3NoMatchResponseFromMatchingService_shouldNotLogCycle3MatchEventToEventSink() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
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
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
        final ResponseFromHub responseFromHub = controller.getErrorResponse();

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test(expected=StateProcessingValidationException.class)
    public void responseFromMatchingService_shouldThrowExceptionWhenInResponseToDoesNotMatchFromCycle3MatchRequest() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);

        MatchFromMatchingService matchFromMatchingService = new MatchFromMatchingService(matchingServiceEntityId, "definitelyNotTheSameRequestId", "assertionBlob", Optional.of(LevelOfAssurance.LEVEL_1));
        controller.handleMatchResponseFromMatchingService(matchFromMatchingService);

    }

    @Test
    public void statusShouldSendNoMatchResponseToTransaction_whenNoMatchResponseSentFromMatchingServiceCycle3Match() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
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
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(5));

        ResponseProcessingDetails responseProcessingDetails = controller.getResponseProcessingDetails();

        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.WAIT);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    public void shouldReturnWaitResponseWhenAskedAndInCycle3MatchRequestSentState() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(5));

        final ResponseProcessingDetails responseProcessingDetails = controller.getResponseProcessingDetails();
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.WAIT);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInCycle3MatchRequestSentState() {
        final String requestId = "requestId";
        final SessionId sessionId = SessionId.createNewSessionId();
        EidasCycle3MatchRequestSentState state = anEidasCycle3MatchRequestSentState().withSessionId(sessionId).withRequestId(requestId).build();
        EidasCycle3MatchRequestSentStateController controller =
                new EidasCycle3MatchRequestSentStateController(state, hubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory,
                        attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(5));

        final ResponseFromHub responseFromHub = controller.getErrorResponse();
        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

}
