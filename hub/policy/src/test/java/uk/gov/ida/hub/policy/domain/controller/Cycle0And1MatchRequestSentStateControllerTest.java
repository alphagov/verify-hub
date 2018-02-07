package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.MatchingProcess;
import uk.gov.ida.hub.policy.domain.NoMatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentStateTransitional;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.policy.builder.domain.MatchFromMatchingServiceBuilder.aMatchFromMatchingService;
import static uk.gov.ida.hub.policy.builder.state.Cycle0And1MatchRequestSentStateBuilder.aCycle0And1MatchRequestSentState;

@RunWith(MockitoJUnitRunner.class)
public class Cycle0And1MatchRequestSentStateControllerTest {

    private static final String TRANSACTION_ENTITY_ID = UUID.randomUUID().toString();
    private static final String MATCHING_SERVICE_ENTITY_ID = UUID.randomUUID().toString();
    private static final String REQUEST_ID = "requestId";

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    @Mock
    private EventSinkHubEventLogger eventSinkHubEventLogger;

    private ResponseFromHubFactory responseFromHubFactory = new ResponseFromHubFactory(new IdGenerator());

    @Mock
    private AssertionRestrictionsFactory assertionRestrictionFactory;

    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    @Mock
    private PolicyConfiguration policyConfiguration;

    @Mock
    private AttributeQueryService attributeQueryService;

    @Mock
    private MatchingProcess matchingProcess;

    public Cycle0And1MatchRequestSentStateController controller;
    private Cycle0And1MatchRequestSentStateTransitional state;

    @Captor
    ArgumentCaptor<AttributeQueryRequestDto> attributeQueryRequestCaptor = null;

    @Mock
    private StateTransitionAction stateTransitionAction;

    @Before
    public void setUp() throws Exception {
        state = aCycle0And1MatchRequestSentState()
                .withMatchingServiceEntityId(MATCHING_SERVICE_ENTITY_ID)
                .withRequestIssuerEntityId(TRANSACTION_ENTITY_ID)
                .build();

        controller = new Cycle0And1MatchRequestSentStateController(
                state,
                eventSinkHubEventLogger,
                stateTransitionAction,
                policyConfiguration,
                mock(LevelOfAssuranceValidator.class),
                transactionsConfigProxy,
                responseFromHubFactory,
                assertionRestrictionFactory,
                matchingServiceConfigProxy,
                attributeQueryService
        );
    }

    @Test
    public void getNextState_shouldThrowStateProcessingValidationExceptionIfResponseIsNotFromTheExpectedMatchingService() throws Exception {
        final String responseIssuerId = "wrong issuer";
        MatchFromMatchingService matchFromMatchingService =
                aMatchFromMatchingService()
                        .withIssuerId(responseIssuerId)
                        .build();
        try {
            controller.validateResponse(matchFromMatchingService);
            fail("fail");
        } catch (StateProcessingValidationException e) {
            assertThat(e.getMessage()).isEqualTo(
                    format("Response to request ID [requestId] came from [{0}] and was expected to come from [{1}]", responseIssuerId, MATCHING_SERVICE_ENTITY_ID));
        }
    }

    @Test
    public void getNextState_shouldReturnNoMatchStateWhenTransactionDoesNotSupportCycle3AndMatchingServiceReturnsNoMatchAndUserAccountCreationIsDisabled() throws Exception {
        //Given
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID))
                .thenReturn(new MatchingProcess(Optional.<String>absent()));

        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID))
                .thenReturn(Collections.<UserAccountCreationAttribute>emptyList());

        //When
        final State nextState = controller.getNextStateForNoMatch();

        //Then
        assertThat(nextState).isInstanceOf(NoMatchState.class);
    }

    @Test
    public void getNextState_shouldReturnUserAccountCreationRequestSentStateWhenTransactionDoesNotSupportCycle3AndSupportsUserAccountCreation() throws Exception {
        //Given
        URI userAccountCreationUri = URI.create("a-test-user-account-creation-uri");
        List<UserAccountCreationAttribute> userAccountCreationAttributes = asList(UserAccountCreationAttribute.DATE_OF_BIRTH);

        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID))
                .thenReturn(new MatchingProcess(Optional.<String>absent()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID))
                .thenReturn(userAccountCreationAttributes);
        when(matchingServiceConfigProxy.getMatchingService(anyString()))
                .thenReturn(aMatchingServiceConfigEntityDataDto().withUserAccountCreationUri(userAccountCreationUri).build());

        //When
        final State nextState = controller.getNextStateForNoMatch();

        //Then
        verify(eventSinkHubEventLogger).logMatchingServiceUserAccountCreationRequestSentEvent(
                state.getSessionId(), TRANSACTION_ENTITY_ID, state.getSessionExpiryTimestamp(), state.getRequestId());
        verify(attributeQueryService).sendAttributeQueryRequest(eq(nextState.getSessionId()), attributeQueryRequestCaptor.capture());

        AttributeQueryRequestDto actualAttributeQueryRequestDto = attributeQueryRequestCaptor.getValue();
        assertThat(actualAttributeQueryRequestDto.getAttributeQueryUri()).isEqualTo(userAccountCreationUri);
        assertThat(actualAttributeQueryRequestDto.getUserAccountCreationAttributes()).isEqualTo(Optional.fromNullable(userAccountCreationAttributes));

        assertThat(nextState).isInstanceOf(UserAccountCreationRequestSentStateTransitional.class);
    }

    @Test
    public void getNextState_shouldGetAwaitingCycle3DataStateWhenTransactionSupportsCycle3() throws Exception {
        //Given
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(matchingProcess);
        when(matchingProcess.getAttributeName()).thenReturn(Optional.of("somestring"));

        //When
        final State nextState = controller.getNextStateForNoMatch();

        //Then
        assertThat(nextState).isInstanceOf(AwaitingCycle3DataState.class);
        assertThat(((AwaitingCycle3DataState)nextState).getEncryptedMatchingDatasetAssertion()).isEqualTo(state.getEncryptedMatchingDatasetAssertion());
                verify(transactionsConfigProxy, times(0)).getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID);
    }

    @Test
    public void cycle0And1NoMatchResponseFromMatchingService_shouldLogRelevantEvents() throws Exception {
        // Given
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(new MatchingProcess(Optional.<String>absent()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID)).thenReturn(Collections.emptyList());
        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID);

        // When
        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        // Then
        verify(eventSinkHubEventLogger, times(1)).logCycle01NoMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
        verify(eventSinkHubEventLogger, times(0)).logCycle01SuccessfulMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
    }

    @Test
    public void cycle0And1SuccessfulMatchResponseFromMatchingService_shouldLogRelevantEvents() throws Exception {
        List<UserAccountCreationAttribute> userAccountCreationAttributes = Collections.singletonList(UserAccountCreationAttribute.DATE_OF_BIRTH);

        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID))
                .thenReturn(new MatchingProcess(Optional.<String>absent()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID))
                .thenReturn(userAccountCreationAttributes);

        MatchFromMatchingService matchFromMatchingService = new MatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID, "assertionBlob", Optional.of(LevelOfAssurance.LEVEL_1));
        controller.handleMatchResponseFromMatchingService(matchFromMatchingService);

        verify(eventSinkHubEventLogger, times(1)).logCycle01SuccessfulMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
        verify(eventSinkHubEventLogger, times(0)).logCycle01NoMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
    }

    @Test
    public void cycle0And1NoMatchResponseFromMatchingServiceWhenC3Enabled_shouldLogRelevantEvents() throws Exception {
        MatchingProcess matchingProcess = mock(MatchingProcess.class);
        when(matchingProcess.getAttributeName()).thenReturn(Optional.of("BLOCKBUSTER_CARD"));
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(matchingProcess);
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID)).thenReturn(Collections.emptyList());

        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID);
        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(eventSinkHubEventLogger, times(0)).logCycle01NoMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
        verify(eventSinkHubEventLogger, times(1)).logWaitingForCycle3AttributesEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
    }

    @Test
    public void cycle0And1NoMatchResponseFromMatchingServiceWhenC3NotEnabled_shouldNotLogWaitingForCycle3AttributesEventToEventSink() throws Exception {
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(new MatchingProcess(Optional.<String>absent()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID)).thenReturn(Collections.emptyList());

        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID);
        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(eventSinkHubEventLogger, times(0)).logWaitingForCycle3AttributesEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInCycle0And1MatchRequestSentState() throws Exception {
        final ResponseFromHub responseFromHub = controller.getErrorResponse();
        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnWaitResponseWhenAskedAndInCycle0And1MatchRequestSentState() throws Exception {
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(5));
        final ResponseProcessingDetails responseProcessingDetails = controller.getResponseProcessingDetails();
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.WAIT);
    }

    @Test
    public void responseProcessingDetails_shouldReturnCompleteStatus_successResponseSentFromMatchingService() throws Exception {
        ArgumentCaptor<SuccessfulMatchState> argumentCaptor = ArgumentCaptor.forClass(SuccessfulMatchState.class);
        MatchFromMatchingService matchFromMatchingService = new MatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID, "assertionBlob", Optional.of(LevelOfAssurance.LEVEL_1));

        controller.handleMatchResponseFromMatchingService(matchFromMatchingService);

        IdentityProvidersConfigProxy identityProvidersConfigProxy = mock(IdentityProvidersConfigProxy.class);
        verify(stateTransitionAction, times(1)).transitionTo(argumentCaptor.capture());

        SuccessfulMatchStateController successfulMatchStateController = new SuccessfulMatchStateController(argumentCaptor.getValue(), responseFromHubFactory, identityProvidersConfigProxy);
        final ResponseProcessingDetails responseProcessingDetails = successfulMatchStateController.getResponseProcessingDetails();

        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SEND_SUCCESSFUL_MATCH_RESPONSE_TO_TRANSACTION);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(state.getSessionId());
    }

    @Test
    public void responseProcessingDetails_shouldReturnNoMatchStatus_noMatchResponseSentFromMatchingService() throws Exception {
        ArgumentCaptor<NoMatchState> argumentCaptor = ArgumentCaptor.forClass(NoMatchState.class);
        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID);
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(new MatchingProcess(Optional.<String>absent()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID)).thenReturn(Collections.emptyList());

        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(stateTransitionAction, times(1)).transitionTo(argumentCaptor.capture());
        NoMatchStateController noMatchStateController = new NoMatchStateController(argumentCaptor.getValue(), responseFromHubFactory);

        final ResponseProcessingDetails responseProcessingDetails = noMatchStateController.getResponseProcessingDetails();
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SEND_NO_MATCH_RESPONSE_TO_TRANSACTION);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(state.getSessionId());
    }

}
