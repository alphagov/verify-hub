package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.MatchingProcess;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.*;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.state.EidasCycle0And1MatchRequestSentStateBuilder.anEidasCycle0And1MatchRequestSentState;

@RunWith(MockitoJUnitRunner.class)
public class EidasCycle0And1MatchRequestSentStateControllerTest {
    @Mock
    private StateTransitionAction stateTransitionAction;

    @Mock
    private EventSinkHubEventLogger eventSinkHubEventLogger;

    @Mock
    private PolicyConfiguration policyConfiguration;

    @Mock
    private LevelOfAssuranceValidator levelOfAssuranceValidator;

    @Mock
    private ResponseFromHubFactory responseFromHubFactory;

    @Mock
    private AttributeQueryService attributeQueryService;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    private EidasCycle0And1MatchRequestSentStateController eidasCycle0And1MatchRequestSentStateController;
    private EidasCycle0And1MatchRequestSentState state;

    @Before
    public void setUp() {
        state = anEidasCycle0And1MatchRequestSentState().build();
        eidasCycle0And1MatchRequestSentStateController = new EidasCycle0And1MatchRequestSentStateController(
            state,
            stateTransitionAction,
            eventSinkHubEventLogger,
            policyConfiguration,
            levelOfAssuranceValidator,
            responseFromHubFactory,
            attributeQueryService,
            transactionsConfigProxy
        );
    }

    @Test
    public void shouldReturnSuccessfulMatchState() {
        MatchFromMatchingService matchFromMatchingService = new MatchFromMatchingService(
            "issuer",
            "inResponseTo",
            "matchingServiceAssertion",
            Optional.of(LevelOfAssurance.LEVEL_2));
        doNothing().when(eventSinkHubEventLogger).logCycle01SuccessfulMatchEvent(
            state.getSessionId(),
            state.getRequestIssuerEntityId(),
            state.getRequestId(),
            state.getSessionExpiryTimestamp());
        EidasSuccessfulMatchState expectedState = new EidasSuccessfulMatchState(
            state.getRequestId(),
            state.getSessionExpiryTimestamp(),
            state.getIdentityProviderEntityId(),
            matchFromMatchingService.getMatchingServiceAssertion(),
            state.getRelayState(),
            state.getRequestIssuerEntityId(),
            state.getAssertionConsumerServiceUri(),
            state.getSessionId(),
            state.getIdpLevelOfAssurance(),
            state.getTransactionSupportsEidas()
        );

        State actualState = eidasCycle0And1MatchRequestSentStateController.getNextStateForMatch(matchFromMatchingService);

        assertThat(actualState).isEqualTo(expectedState);
    }

    @Test
    public void shouldReturnEidasAwaitingCycle3DataState() {
        when(transactionsConfigProxy.getMatchingProcess(state.getRequestIssuerEntityId())).thenReturn(new MatchingProcess(Optional.of("cycle3AttributeName")));
        doNothing().when(eventSinkHubEventLogger).logWaitingForCycle3AttributesEvent(
            state.getSessionId(),
            state.getRequestIssuerEntityId(),
            state.getRequestId(),
            state.getSessionExpiryTimestamp());
        EidasAwaitingCycle3DataState expectedState = new EidasAwaitingCycle3DataState(
            state.getRequestId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getAssertionConsumerServiceUri(),
            state.getSessionId(),
            state.getTransactionSupportsEidas(),
            state.getIdentityProviderEntityId(),
            state.getMatchingServiceAdapterEntityId(),
            state.getRelayState(),
            state.getPersistentId(),
            state.getIdpLevelOfAssurance(),
            state.getEncryptedIdentityAssertion()
        );

        State actualState = eidasCycle0And1MatchRequestSentStateController.getNextStateForNoMatch();

        assertThat(actualState).isEqualTo(expectedState);
    }

    @Test
    public void shouldReturnNoMatchState() {
        when(transactionsConfigProxy.getMatchingProcess(state.getRequestIssuerEntityId())).thenReturn(new MatchingProcess(Optional.absent()));
        doNothing().when(eventSinkHubEventLogger).logCycle01NoMatchEvent(
            state.getSessionId(),
            state.getRequestIssuerEntityId(),
            state.getRequestId(),
            state.getSessionExpiryTimestamp());
        NoMatchState expectedState = new NoMatchState(
            state.getRequestId(),
            state.getIdentityProviderEntityId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getAssertionConsumerServiceUri(),
            state.getRelayState(),
            state.getSessionId(),
            state.getTransactionSupportsEidas()
        );

        State actualState = eidasCycle0And1MatchRequestSentStateController.getNextStateForNoMatch();

        assertThat(actualState).isEqualTo(expectedState);
    }
}
