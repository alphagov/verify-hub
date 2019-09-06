package uk.gov.ida.hub.policy.domain.controller;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.MatchingProcess;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;
import uk.gov.ida.shared.utils.datetime.DateTimeFreezer;

import java.net.URI;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.policy.builder.state.EidasCycle0And1MatchRequestSentStateBuilder.anEidasCycle0And1MatchRequestSentState;
import static uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute.FIRST_NAME;

@RunWith(MockitoJUnitRunner.class)
public class EidasCycle0And1MatchRequestSentStateControllerTest {
    @Mock
    private StateTransitionAction stateTransitionAction;

    @Mock
    private HubEventLogger hubEventLogger;

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

    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    private EidasCycle0And1MatchRequestSentStateController eidasCycle0And1MatchRequestSentStateController;
    private EidasCycle0And1MatchRequestSentState state;

    @Before
    public void setUp() {
        state = anEidasCycle0And1MatchRequestSentState().build();
        eidasCycle0And1MatchRequestSentStateController = new EidasCycle0And1MatchRequestSentStateController(
            state,
            stateTransitionAction,
            hubEventLogger,
            policyConfiguration,
            levelOfAssuranceValidator,
            responseFromHubFactory,
            attributeQueryService,
            transactionsConfigProxy,
            matchingServiceConfigProxy
        );
        DateTimeFreezer.freezeTime();
    }

    @After
    public void tearDown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void shouldReturnSuccessfulMatchState() {
        final ArgumentCaptor<EidasSuccessfulMatchState> capturedState = ArgumentCaptor.forClass(EidasSuccessfulMatchState.class);

        MatchFromMatchingService matchFromMatchingService = new MatchFromMatchingService(
            state.getMatchingServiceAdapterEntityId(),
            state.getRequestId(),
            "matchingServiceAssertion",
            Optional.of(state.getIdpLevelOfAssurance()));

        doNothing().when(hubEventLogger).logCycle01SuccessfulMatchEvent(
            state.getSessionId(),
            state.getRequestIssuerEntityId(),
            state.getRequestId(),
            state.getSessionExpiryTimestamp());

        EidasSuccessfulMatchState expectedState = new EidasSuccessfulMatchState(
            state.getRequestId(),
            state.getSessionExpiryTimestamp(),
            state.getIdentityProviderEntityId(),
            matchFromMatchingService.getMatchingServiceAssertion(),
            state.getRelayState().orElse(null),
            state.getRequestIssuerEntityId(),
            state.getAssertionConsumerServiceUri(),
            state.getSessionId(),
            state.getIdpLevelOfAssurance(),
            state.getTransactionSupportsEidas()
        );

        eidasCycle0And1MatchRequestSentStateController.handleMatchResponseFromMatchingService(matchFromMatchingService);

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isEqualTo(expectedState);
    }

    @Test
    public void shouldReturnEidasAwaitingCycle3DataState() {
        final ArgumentCaptor<EidasAwaitingCycle3DataState> capturedState = ArgumentCaptor.forClass(EidasAwaitingCycle3DataState.class);
        when(transactionsConfigProxy.getMatchingProcess(state.getRequestIssuerEntityId())).thenReturn(new MatchingProcess(Optional.of("cycle3AttributeName")));
        doNothing().when(hubEventLogger).logWaitingForCycle3AttributesEvent(
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
            state.getRelayState().orElse(null),
            state.getPersistentId(),
            state.getIdpLevelOfAssurance(),
            state.getEncryptedIdentityAssertion(),
            state.getForceAuthentication().orElse(null)
        );

        eidasCycle0And1MatchRequestSentStateController.transitionToNextStateForNoMatchResponse();

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isEqualToComparingFieldByField(expectedState);
    }

    @Test
    public void shouldReturnEidasUserAccountCreationStateWhenUserAccountCreationIsEnabled() {
        final ArgumentCaptor<EidasUserAccountCreationRequestSentState> capturedState = ArgumentCaptor.forClass(EidasUserAccountCreationRequestSentState.class);
        URI userAccountCreationUri = URI.create("a-test-user-account-creation-uri");

        when(transactionsConfigProxy.getMatchingProcess(state.getRequestIssuerEntityId())).thenReturn(new MatchingProcess(Optional.empty()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(state.getRequestIssuerEntityId())).thenReturn(singletonList(FIRST_NAME));
        when(matchingServiceConfigProxy.getMatchingService(anyString()))
                .thenReturn(aMatchingServiceConfigEntityDataDto().withUserAccountCreationUri(userAccountCreationUri).build());
        doNothing().when(hubEventLogger).logMatchingServiceUserAccountCreationRequestSentEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId());

        EidasUserAccountCreationRequestSentState expectedState = new EidasUserAccountCreationRequestSentState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdentityProviderEntityId(),
                state.getRelayState().orElse(null),
                state.getIdpLevelOfAssurance(),
                state.getMatchingServiceAdapterEntityId(),
                state.getForceAuthentication().orElse(null));

        eidasCycle0And1MatchRequestSentStateController.transitionToNextStateForNoMatchResponse();

        verify(attributeQueryService).sendAttributeQueryRequest(eq(state.getSessionId()), any());
        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isEqualToComparingFieldByField(expectedState);
    }

    @Test
    public void shouldReturnNoMatchStateWhenUserAccountCreationIsDisabled() {
        final ArgumentCaptor<NoMatchState> capturedState = ArgumentCaptor.forClass(NoMatchState.class);

        when(transactionsConfigProxy.getMatchingProcess(state.getRequestIssuerEntityId())).thenReturn(new MatchingProcess(Optional.empty()));
        when(transactionsConfigProxy.getUserAccountCreationAttributes(state.getRequestIssuerEntityId())).thenReturn(Lists.emptyList());
        doNothing().when(hubEventLogger).logCycle01NoMatchEvent(
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
            state.getRelayState().orElse(null),
            state.getSessionId(),
            state.getTransactionSupportsEidas()
        );

        eidasCycle0And1MatchRequestSentStateController.transitionToNextStateForNoMatchResponse();

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isEqualToComparingFieldByField(expectedState);
    }
}
