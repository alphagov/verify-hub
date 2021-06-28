package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
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
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.ida.hub.policy.builder.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.policy.builder.domain.MatchFromMatchingServiceBuilder.aMatchFromMatchingService;
import static uk.gov.ida.hub.policy.builder.state.Cycle0And1MatchRequestSentStateBuilder.aCycle0And1MatchRequestSentState;

@ExtendWith(MockitoExtension.class)
public class Cycle0And1MatchRequestSentStateControllerTest {

    private static final String TRANSACTION_ENTITY_ID = UUID.randomUUID().toString();
    private static final String MATCHING_SERVICE_ENTITY_ID = UUID.randomUUID().toString();
    private static final String REQUEST_ID = "requestId";

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    @Mock
    private HubEventLogger hubEventLogger;

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
    private Cycle0And1MatchRequestSentState state;

    @Captor
    private ArgumentCaptor<AttributeQueryRequestDto> attributeQueryRequestCaptor = null;

    @Mock
    private StateTransitionAction stateTransitionAction;

    @BeforeEach
    public void setUp() {
        state = aCycle0And1MatchRequestSentState()
                .withMatchingServiceEntityId(MATCHING_SERVICE_ENTITY_ID)
                .withRequestIssuerEntityId(TRANSACTION_ENTITY_ID)
                .build();

        controller = new Cycle0And1MatchRequestSentStateController(
                state,
                hubEventLogger,
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
    public void shouldThrowStateProcessingValidationExceptionIfResponseIsNotFromTheExpectedMatchingService() {
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
    public void houldReturnNoMatchStateWhenTransactionDoesNotSupportCycle3AndMatchingServiceReturnsNoMatchAndUserAccountCreationIsDisabled() {
        final ArgumentCaptor<NoMatchState> capturedState = ArgumentCaptor.forClass(NoMatchState.class);

        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID))
                .thenReturn(new MatchingProcess(Optional.empty()));

        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID))
                .thenReturn(Collections.emptyList());

        controller.transitionToNextStateForNoMatchResponse();

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isInstanceOf(NoMatchState.class);
    }

    @Test
    public void shouldReturnUserAccountCreationRequestSentStateWhenNoMatchAndTransactionDoesNotSupportCycle3AndSupportsUserAccountCreation() {
        URI userAccountCreationUri = URI.create("a-test-user-account-creation-uri");
        List<UserAccountCreationAttribute> userAccountCreationAttributes = singletonList(UserAccountCreationAttribute.DATE_OF_BIRTH);
        final ArgumentCaptor<UserAccountCreationRequestSentState> capturedState = ArgumentCaptor.forClass(UserAccountCreationRequestSentState.class);

        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID))
                .thenReturn(new MatchingProcess(Optional.empty()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID))
                .thenReturn(userAccountCreationAttributes);
        when(matchingServiceConfigProxy.getMatchingService(anyString()))
                .thenReturn(aMatchingServiceConfigEntityDataDto().withUserAccountCreationUri(userAccountCreationUri).build());

        controller.transitionToNextStateForNoMatchResponse();

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        verify(hubEventLogger).logMatchingServiceUserAccountCreationRequestSentEvent(
                state.getSessionId(), TRANSACTION_ENTITY_ID, state.getSessionExpiryTimestamp(), state.getRequestId());
        verify(attributeQueryService).sendAttributeQueryRequest(eq(capturedState.getValue().getSessionId()), attributeQueryRequestCaptor.capture());

        AttributeQueryRequestDto actualAttributeQueryRequestDto = attributeQueryRequestCaptor.getValue();
        assertThat(actualAttributeQueryRequestDto.getAttributeQueryUri()).isEqualTo(userAccountCreationUri);
        assertThat(actualAttributeQueryRequestDto.getUserAccountCreationAttributes()).isEqualTo(Optional.ofNullable(userAccountCreationAttributes));

        assertThat(capturedState.getValue()).isInstanceOf(UserAccountCreationRequestSentState.class);
    }

    @Test
    public void shouldGetAwaitingCycle3DataStateWhenNoMatchAndTransactionSupportsCycle3() {
        final ArgumentCaptor<AwaitingCycle3DataState> capturedState = ArgumentCaptor.forClass(AwaitingCycle3DataState.class);

        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(matchingProcess);
        when(matchingProcess.getAttributeName()).thenReturn(Optional.of("somestring"));

        controller.transitionToNextStateForNoMatchResponse();

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isInstanceOf(AwaitingCycle3DataState.class);
        assertThat((capturedState.getValue()).getEncryptedMatchingDatasetAssertion()).isEqualTo(state.getEncryptedMatchingDatasetAssertion());
                verify(transactionsConfigProxy, times(0)).getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID);
    }

    @Test
    public void shouldLogRelevantEventsWhenReceivedCycle0And1NoMatchResponseFromMatchingService() {
        // Given
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(new MatchingProcess(Optional.empty()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID)).thenReturn(emptyList());
        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID);

        // When
        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        // Then
        verify(hubEventLogger, times(1)).logCycle01NoMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
        verify(hubEventLogger, times(0)).logCycle01SuccessfulMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
    }

    @Test
    public void shouldLogRelevantEventsWhenReceivedCycle0And1SuccessfulMatchResponseFromMatchingService() {
        List<UserAccountCreationAttribute> userAccountCreationAttributes = singletonList(UserAccountCreationAttribute.DATE_OF_BIRTH);

        MatchFromMatchingService matchFromMatchingService = new MatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID, "assertionBlob", Optional.of(LevelOfAssurance.LEVEL_1));
        controller.handleMatchResponseFromMatchingService(matchFromMatchingService);

        verify(hubEventLogger, times(1)).logCycle01SuccessfulMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
        verify(hubEventLogger, times(0)).logCycle01NoMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
    }

    @Test
    public void shouldLogRelevantEventsWhenReceivedCycle0And1NoMatchResponseFromMatchingServiceAndC3Enabled() {
        MatchingProcess matchingProcess = mock(MatchingProcess.class);
        when(matchingProcess.getAttributeName()).thenReturn(Optional.of("BLOCKBUSTER_CARD"));
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(matchingProcess);

        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID);
        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(hubEventLogger, times(0)).logCycle01NoMatchEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
        verify(hubEventLogger, times(1)).logWaitingForCycle3AttributesEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
    }

    @Test
    public void shouldNotLogWaitingForCycle3AttributesEventToEventSinkWhenReceivedCycle0And1NoMatchResponseAndC3NotEnabled() {
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(new MatchingProcess(Optional.empty()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID)).thenReturn(emptyList());

        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID);
        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(hubEventLogger, times(0)).logWaitingForCycle3AttributesEvent(state.getSessionId(), TRANSACTION_ENTITY_ID, REQUEST_ID, state.getSessionExpiryTimestamp());
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInCycle0And1MatchRequestSentState() {
        final ResponseFromHub responseFromHub = controller.getErrorResponse();
        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnWaitResponseWhenAskedAndInCycle0And1MatchRequestSentState() {
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(Duration.standardMinutes(5));
        final ResponseProcessingDetails responseProcessingDetails = controller.getResponseProcessingDetails();
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.WAIT);
    }

    @Test
    public void shouldReturnCompleteStatusWhenSuccessResponseReceivedFromMatchingService() {
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
    public void shouldReturnNoMatchStatusWhenNoMatchResponseReceivedFromMatchingService() {
        ArgumentCaptor<NoMatchState> argumentCaptor = ArgumentCaptor.forClass(NoMatchState.class);
        NoMatchFromMatchingService noMatchFromMatchingService = new NoMatchFromMatchingService(MATCHING_SERVICE_ENTITY_ID, REQUEST_ID);
        when(transactionsConfigProxy.getMatchingProcess(TRANSACTION_ENTITY_ID)).thenReturn(new MatchingProcess(Optional.empty()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(TRANSACTION_ENTITY_ID)).thenReturn(emptyList());

        controller.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);

        verify(stateTransitionAction, times(1)).transitionTo(argumentCaptor.capture());
        NoMatchStateController noMatchStateController = new NoMatchStateController(argumentCaptor.getValue(), responseFromHubFactory);

        final ResponseProcessingDetails responseProcessingDetails = noMatchStateController.getResponseProcessingDetails();
        assertThat(responseProcessingDetails.getResponseProcessingStatus()).isEqualTo(ResponseProcessingStatus.SEND_NO_MATCH_RESPONSE_TO_TRANSACTION);
        assertThat(responseProcessingDetails.getSessionId()).isEqualTo(state.getSessionId());
    }
}
