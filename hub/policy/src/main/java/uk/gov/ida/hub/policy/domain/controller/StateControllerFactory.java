package uk.gov.ida.hub.policy.domain.controller;

import com.google.inject.Injector;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.PolicyState;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.services.CountriesService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import javax.inject.Inject;

import static java.text.MessageFormat.format;

public class StateControllerFactory {

    private final Injector injector;

    @Inject
    public StateControllerFactory(Injector injector) {
        this.injector = injector;
    }

    public <T extends State> StateController build(final T state, final StateTransitionAction stateTransitionAction) {
        PolicyState policyState = PolicyState.fromStateClass(state.getClass());
        switch (policyState) {

            case SESSION_STARTED:
                return new SessionStartedStateController(
                        (SessionStartedState) state,
                        injector.getInstance(HubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class));

            case COUNTRY_SELECTED:
                return new CountrySelectedStateController(
                        (CountrySelectedState) state,
                        injector.getInstance(HubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class));

            case IDP_SELECTED:
                return new IdpSelectedStateController(
                        (IdpSelectedState) state,
                        injector.getInstance(HubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(PolicyConfiguration.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(MatchingServiceConfigProxy.class));

            case CYCLE_0_AND_1_MATCH_REQUEST_SENT:
                return new Cycle0And1MatchRequestSentStateController(
                        (Cycle0And1MatchRequestSentState) state,
                        injector.getInstance(HubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(MatchingServiceConfigProxy.class),
                        injector.getInstance(AttributeQueryService.class));

            case EIDAS_CYCLE_0_AND_1_MATCH_REQUEST_SENT:
                return new EidasCycle0And1MatchRequestSentStateController(
                        (EidasCycle0And1MatchRequestSentState) state,
                        stateTransitionAction,
                        injector.getInstance(HubEventLogger.class),
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(AttributeQueryService.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(MatchingServiceConfigProxy.class));

            case SUCCESSFUL_MATCH:
                return new SuccessfulMatchStateController(
                        (SuccessfulMatchState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class));

            case EIDAS_SUCCESSFUL_MATCH:
                return new EidasSuccessfulMatchStateController(
                        (EidasSuccessfulMatchState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(CountriesService.class));

            case NO_MATCH:
                return new NoMatchStateController(
                        (NoMatchState) state,
                        injector.getInstance(ResponseFromHubFactory.class));

            case USER_ACCOUNT_CREATED:
                return new UserAccountCreatedStateController(
                        (UserAccountCreatedState) state,
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class));

            case AWAITING_CYCLE3_DATA:
                return new AwaitingCycle3DataStateController(
                        (AwaitingCycle3DataState) state,
                        injector.getInstance(HubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(PolicyConfiguration.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(MatchingServiceConfigProxy.class));

            case EIDAS_AWAITING_CYCLE3_DATA:
                return new EidasAwaitingCycle3DataStateController(
                        (EidasAwaitingCycle3DataState) state,
                        injector.getInstance(HubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(PolicyConfiguration.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(MatchingServiceConfigProxy.class));

            case CYCLE3_MATCH_REQUEST_SENT:
                return new Cycle3MatchRequestSentStateController(
                        (Cycle3MatchRequestSentState) state,
                        injector.getInstance(HubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(MatchingServiceConfigProxy.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(AttributeQueryService.class));

            case EIDAS_CYCLE_3_MATCH_REQUEST_SENT:
                return new EidasCycle3MatchRequestSentStateController(
                        (EidasCycle3MatchRequestSentState) state,
                        injector.getInstance(HubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(AttributeQueryService.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(MatchingServiceConfigProxy.class));

            case TIMEOUT:
                return new TimeoutStateController(
                        (TimeoutState) state,
                        injector.getInstance(ResponseFromHubFactory.class));

            case MATCHING_SERVICE_REQUEST_ERROR:
                return new MatchingServiceRequestErrorStateController(
                        (MatchingServiceRequestErrorState) state,
                        injector.getInstance(ResponseFromHubFactory.class));

            case USER_ACCOUNT_CREATION_REQUEST_SENT:
                return new UserAccountCreationRequestSentStateController(
                        (UserAccountCreationRequestSentState) state,
                        stateTransitionAction,
                        injector.getInstance(HubEventLogger.class),
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(AttributeQueryService.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(MatchingServiceConfigProxy.class));

            case AUTHN_FAILED_ERROR:
                return new AuthnFailedErrorStateController(
                        (AuthnFailedErrorState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(HubEventLogger.class));

            case FRAUD_EVENT_DETECTED:
                return new FraudEventDetectedStateController(
                        (FraudEventDetectedState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(HubEventLogger.class));

            case REQUESTER_ERROR:
                return new RequesterErrorStateController(
                        (RequesterErrorState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(HubEventLogger.class));

            case CYCLE_3_DATA_INPUT_CANCELLED:
                return new Cycle3DataInputCancelledStateController(
                        (Cycle3DataInputCancelledState) state,
                        injector.getInstance(ResponseFromHubFactory.class));

            case USER_ACCOUNT_CREATION_FAILED:
                return new UserAccountCreationFailedStateController(
                        (UserAccountCreationFailedState) state,
                        injector.getInstance(ResponseFromHubFactory.class));

            default:
                throw new IllegalStateException(format("Invalid state controller class for {0}", policyState));
        }
    }
}
