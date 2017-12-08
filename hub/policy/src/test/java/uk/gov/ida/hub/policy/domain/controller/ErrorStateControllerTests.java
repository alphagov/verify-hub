package uk.gov.ida.hub.policy.domain.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.builder.state.AuthnFailedErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.AwaitingCycle3DataStateBuilder;
import uk.gov.ida.hub.policy.builder.state.Cycle0And1MatchRequestSentStateBuilder;
import uk.gov.ida.hub.policy.builder.state.Cycle3DataInputCancelledStateBuilder;
import uk.gov.ida.hub.policy.builder.state.Cycle3MatchRequestSentStateBuilder;
import uk.gov.ida.hub.policy.builder.state.FraudEventDetectedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.MatchingServiceRequestErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.NoMatchStateBuilder;
import uk.gov.ida.hub.policy.builder.state.RequesterErrorStateBuilder;
import uk.gov.ida.hub.policy.builder.state.SessionStartedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.SuccessfulMatchStateBuilder;
import uk.gov.ida.hub.policy.builder.state.TimeoutStateBuilder;
import uk.gov.ida.hub.policy.builder.state.UserAccountCreatedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.UserAccountCreationFailedStateBuilder;
import uk.gov.ida.hub.policy.builder.state.UserAccountCreationRequestSentStateBuilder;
import uk.gov.ida.hub.policy.controllogic.AuthnRequestFromTransactionHandler;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateFactory;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ErrorStateControllerTests {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionStartedStateFactory sessionStartedStateFactory;
    @Mock
    private EventSinkHubEventLogger eventSinkHubEventLogger;
    @Mock
    private PolicyConfiguration policyConfiguration;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;
    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private AssertionRestrictionsFactory assertionRestrictionFactory;
    @Mock
    private LevelOfAssuranceValidator levelOfAssuranceValidator;
    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;
    @Mock
    private AttributeQueryService attributeQueryService;

    private SessionId sessionId;

    private ResponseFromHubFactory responseFromHubFactory = new ResponseFromHubFactory(new IdGenerator());

    private AuthnRequestFromTransactionHandler authnRequestFromTransactionHandler;

    @Before
    public void setUp() {
        sessionId = SessionId.createNewSessionId();
        authnRequestFromTransactionHandler = new AuthnRequestFromTransactionHandler(sessionRepository, sessionStartedStateFactory, eventSinkHubEventLogger, policyConfiguration, transactionsConfigProxy);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInSessionStartedState() throws Exception {
        SessionStartedState state = SessionStartedStateBuilder.aSessionStartedState().build();
        StateController stateController = new SessionStartedStateController(state, eventSinkHubEventLogger, stateTransitionAction, transactionsConfigProxy, responseFromHubFactory, identityProvidersConfigProxy);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInAuthnFailedErrorState() throws Exception {
        AuthnFailedErrorState state = AuthnFailedErrorStateBuilder.anAuthnFailedErrorState().build();
        StateController stateController = new AuthnFailedErrorStateController(state, responseFromHubFactory, stateTransitionAction, sessionStartedStateFactory, transactionsConfigProxy, identityProvidersConfigProxy, eventSinkHubEventLogger);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInRequesterErrorState() throws Exception {
        RequesterErrorState state = RequesterErrorStateBuilder.aRequesterErrorState().build();
        StateController stateController = new RequesterErrorStateController(state, responseFromHubFactory, stateTransitionAction, transactionsConfigProxy, identityProvidersConfigProxy, eventSinkHubEventLogger);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInCycle0And1MatchRequestSentState() throws Exception {
        Cycle0And1MatchRequestSentState state = Cycle0And1MatchRequestSentStateBuilder.aCycle0And1MatchRequestSentState().build();
        StateController stateController = new Cycle0And1MatchRequestSentStateController(state, eventSinkHubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, transactionsConfigProxy, responseFromHubFactory, assertionRestrictionFactory, matchingServiceConfigProxy, attributeQueryService);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInAwaitingCycle3DataState() throws Exception {
        AwaitingCycle3DataState state = AwaitingCycle3DataStateBuilder.anAwaitingCycle3DataState().build();
        StateController stateController = new AwaitingCycle3DataStateController(state, eventSinkHubEventLogger, stateTransitionAction, transactionsConfigProxy, responseFromHubFactory, policyConfiguration, assertionRestrictionFactory, matchingServiceConfigProxy);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInCycle3DataInputCancelledState() throws Exception {
        Cycle3DataInputCancelledState state = Cycle3DataInputCancelledStateBuilder.aCycle3DataInputCancelledState().build();
        StateController stateController = new Cycle3DataInputCancelledStateController(state, responseFromHubFactory);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInCycle3MatchRequestSentState() throws Exception {
        Cycle3MatchRequestSentState state = Cycle3MatchRequestSentStateBuilder.aCycle3MatchRequestSentState().build();
        StateController stateController = new Cycle3MatchRequestSentStateController(state, eventSinkHubEventLogger, stateTransitionAction, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory, transactionsConfigProxy, matchingServiceConfigProxy, assertionRestrictionFactory, attributeQueryService);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInSuccessfulMatchState() throws Exception {
        SuccessfulMatchState state = SuccessfulMatchStateBuilder.aSuccessfulMatchState().build();
        StateController stateController = new SuccessfulMatchStateController(state, responseFromHubFactory, identityProvidersConfigProxy);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInMatchingServiceRequestErrorState() throws Exception {
        MatchingServiceRequestErrorState state = MatchingServiceRequestErrorStateBuilder.aMatchingServiceRequestErrorState().build();
        StateController stateController = new MatchingServiceRequestErrorStateController(state, responseFromHubFactory);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInNoMatchState() throws Exception {
        NoMatchState state = NoMatchStateBuilder.aNoMatchState().build();
        StateController stateController = new NoMatchStateController(state, responseFromHubFactory);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInUserAccountCreationRequestSentState() throws Exception {
        UserAccountCreationRequestSentState state = UserAccountCreationRequestSentStateBuilder.aUserAccountCreationRequestSentState().build();
        StateController stateController = new UserAccountCreationRequestSentStateController(state, stateTransitionAction, eventSinkHubEventLogger, policyConfiguration, levelOfAssuranceValidator, responseFromHubFactory, attributeQueryService);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }


    @Test
    public void shouldReturnErrorResponseWhenAskedAndInFraudEventDetectedState() throws Exception {
        FraudEventDetectedState state = FraudEventDetectedStateBuilder.aFraudEventDetectedState().build();
        StateController stateController = new FraudEventDetectedStateController(state, responseFromHubFactory, stateTransitionAction, sessionStartedStateFactory, null, null, null);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInIdpSelectedState() throws Exception {
        IdpSelectedState state = IdpSelectedStateBuilder.anIdpSelectedState().build();
        StateController stateController = new IdpSelectedStateController(state, sessionStartedStateFactory, eventSinkHubEventLogger, stateTransitionAction, identityProvidersConfigProxy, transactionsConfigProxy, responseFromHubFactory, policyConfiguration, assertionRestrictionFactory, matchingServiceConfigProxy);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInTimeoutState() throws Exception {
        TimeoutState state = TimeoutStateBuilder.aTimeoutState().build();
        StateController stateController = new TimeoutStateController(state, responseFromHubFactory);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndInUserAccountCreatedState() throws Exception {
        UserAccountCreatedState state = UserAccountCreatedStateBuilder.aUserAccountCreatedState().build();
        StateController stateController = new UserAccountCreatedStateController(state, identityProvidersConfigProxy, responseFromHubFactory);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }

    @Test
    public void shouldReturnErrorResponseWhenAskedAndUserAccountCreationFailedState() throws Exception {
        UserAccountCreationFailedState state = UserAccountCreationFailedStateBuilder.aUserAccountCreationFailedState().build();
        StateController stateController = new UserAccountCreationFailedStateController(state, responseFromHubFactory);
        when(sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class)).thenReturn(stateController);

        ResponseFromHub responseFromHub = authnRequestFromTransactionHandler.getErrorResponseFromHub(sessionId);

        assertThat(responseFromHub.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
    }
}
