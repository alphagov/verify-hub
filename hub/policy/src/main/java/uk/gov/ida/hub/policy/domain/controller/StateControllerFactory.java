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
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataStateTransitional;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchStateTransitional;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentStateTransitional;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
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
            // Deprecated
            case SESSION_STARTED_TRANSITIONAL:
                final SessionStartedStateTransitional startedStateTransitional = (SessionStartedStateTransitional) state;
                return new SessionStartedStateController(
                    new SessionStartedState(
                            startedStateTransitional.getRequestId(),
                            startedStateTransitional.getRelayState().orNull(),
                            startedStateTransitional.getRequestIssuerEntityId(),
                            startedStateTransitional.getAssertionConsumerServiceUri(),
                            startedStateTransitional.getForceAuthentication().orNull(),
                            startedStateTransitional.getSessionExpiryTimestamp(),
                            startedStateTransitional.getSessionId(),
                            startedStateTransitional.getTransactionSupportsEidas()
                    ),
                    injector.getInstance(EventSinkHubEventLogger.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class));

            case SESSION_STARTED:
                return new SessionStartedStateController(
                        (SessionStartedState) state,
                        injector.getInstance(EventSinkHubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class));

            case COUNTRY_SELECTED:
                return new CountrySelectedStateController(
                        (CountrySelectedState) state,
                        injector.getInstance(EventSinkHubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class));

            // Deprecated
            case IDP_SELECTED_TRANSITIONAL:
                final IdpSelectedStateTransitional idpSelectedStateTransitional = (IdpSelectedStateTransitional) state;
                return new IdpSelectedStateController(
                    new IdpSelectedState(
                            idpSelectedStateTransitional.getRequestId(),
                            idpSelectedStateTransitional.getIdpEntityId(),
                            idpSelectedStateTransitional.getMatchingServiceEntityId(),
                            idpSelectedStateTransitional.getLevelsOfAssurance(),
                            idpSelectedStateTransitional.getUseExactComparisonType(),
                            idpSelectedStateTransitional.getForceAuthentication().orNull(),
                            idpSelectedStateTransitional.getAssertionConsumerServiceUri(),
                            idpSelectedStateTransitional.getRequestIssuerEntityId(),
                            idpSelectedStateTransitional.getRelayState().orNull(),
                            idpSelectedStateTransitional.getSessionExpiryTimestamp(),
                            idpSelectedStateTransitional.isRegistering(),
                            idpSelectedStateTransitional.getRequestedLoa(),
                            idpSelectedStateTransitional.getSessionId(),
                            idpSelectedStateTransitional.getAvailableIdentityProviderEntityIds(),
                            idpSelectedStateTransitional.getTransactionSupportsEidas()
                    ),
                    injector.getInstance(EventSinkHubEventLogger.class),
                    stateTransitionAction,
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(PolicyConfiguration.class),
                    injector.getInstance(AssertionRestrictionsFactory.class),
                    injector.getInstance(MatchingServiceConfigProxy.class)
                );

            case IDP_SELECTED:
                return new IdpSelectedStateController(
                        (IdpSelectedState) state,
                        injector.getInstance(EventSinkHubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(PolicyConfiguration.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(MatchingServiceConfigProxy.class)
                );

            // Deprecated
            case CYCLE_0_AND_1_MATCH_REQUEST_SENT_TRANSITIONAL:
                return new Cycle0And1MatchRequestSentStateController(
                    (Cycle0And1MatchRequestSentStateTransitional) state,
                    injector.getInstance(EventSinkHubEventLogger.class),
                    stateTransitionAction,
                    injector.getInstance(PolicyConfiguration.class),
                    new LevelOfAssuranceValidator(),
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(AssertionRestrictionsFactory.class),
                    injector.getInstance(MatchingServiceConfigProxy.class),
                    injector.getInstance(AttributeQueryService.class)
                );

            case CYCLE_0_AND_1_MATCH_REQUEST_SENT:
                final Cycle0And1MatchRequestSentState cycle0And1MatchRequestSentState = (Cycle0And1MatchRequestSentState) state;
                return new Cycle0And1MatchRequestSentStateController(
                        new Cycle0And1MatchRequestSentStateTransitional(
                                cycle0And1MatchRequestSentState.getRequestId(),
                                cycle0And1MatchRequestSentState.getRequestIssuerEntityId(),
                                cycle0And1MatchRequestSentState.getSessionExpiryTimestamp(),
                                cycle0And1MatchRequestSentState.getAssertionConsumerServiceUri(),
                                cycle0And1MatchRequestSentState.getSessionId(),
                                cycle0And1MatchRequestSentState.getTransactionSupportsEidas(),
                                cycle0And1MatchRequestSentState.isRegistering(),
                                cycle0And1MatchRequestSentState.getIdentityProviderEntityId(),
                                cycle0And1MatchRequestSentState.getRelayState(),
                                cycle0And1MatchRequestSentState.getIdpLevelOfAssurance(),
                                cycle0And1MatchRequestSentState.getMatchingServiceAdapterEntityId(),
                                cycle0And1MatchRequestSentState.getEncryptedMatchingDatasetAssertion(),
                                cycle0And1MatchRequestSentState.getAuthnStatementAssertion(),
                                cycle0And1MatchRequestSentState.getPersistentId(),
                                cycle0And1MatchRequestSentState.getRequestSentTime()
                        ),
                        injector.getInstance(EventSinkHubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(MatchingServiceConfigProxy.class),
                        injector.getInstance(AttributeQueryService.class)
                );

            case EIDAS_CYCLE_0_AND_1_MATCH_REQUEST_SENT:
                return new EidasCycle0And1MatchRequestSentStateController(
                    (EidasCycle0And1MatchRequestSentState) state,
                    stateTransitionAction,
                    injector.getInstance(EventSinkHubEventLogger.class),
                    injector.getInstance(PolicyConfiguration.class),
                    new LevelOfAssuranceValidator(),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(AttributeQueryService.class),
                    injector.getInstance(TransactionsConfigProxy.class)
                );

            // Deprecated
            case SUCCESSFUL_MATCH_TRANSITIONAL:
                final SuccessfulMatchStateTransitional successfulMatchStateTransitional = (SuccessfulMatchStateTransitional) state;
                return new SuccessfulMatchStateController(
                    new SuccessfulMatchState(
                            successfulMatchStateTransitional.getRequestId(),
                            successfulMatchStateTransitional.getSessionExpiryTimestamp(),
                            successfulMatchStateTransitional.getIdentityProviderEntityId(),
                            successfulMatchStateTransitional.getMatchingServiceAssertion(),
                            successfulMatchStateTransitional.getRelayState().orNull(),
                            successfulMatchStateTransitional.getRequestIssuerEntityId(),
                            successfulMatchStateTransitional.getAssertionConsumerServiceUri(),
                            successfulMatchStateTransitional.getSessionId(),
                            successfulMatchStateTransitional.getLevelOfAssurance(),
                            successfulMatchStateTransitional.isRegistering(),
                            successfulMatchStateTransitional.getTransactionSupportsEidas()
                    ),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class)
                );

            case SUCCESSFUL_MATCH:
                return new SuccessfulMatchStateController(
                        (SuccessfulMatchState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class)
                );

            case EIDAS_SUCCESSFUL_MATCH:
                return new EidasSuccessfulMatchStateController(
                    (EidasSuccessfulMatchState) state,
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(CountriesService.class)
                );
            case NO_MATCH:
                return new NoMatchStateController(
                    (NoMatchState) state,
                    injector.getInstance(ResponseFromHubFactory.class)
                );

            // Deprecated
            case USER_ACCOUNT_CREATED_TRANSITIONAL:
                final UserAccountCreatedStateTransitional userAccountCreatedStateTransitional = (UserAccountCreatedStateTransitional) state;
                return new UserAccountCreatedStateController(
                    new UserAccountCreatedState(
                            userAccountCreatedStateTransitional.getRequestId(),
                            userAccountCreatedStateTransitional.getRequestIssuerEntityId(),
                            userAccountCreatedStateTransitional.getSessionExpiryTimestamp(),
                            userAccountCreatedStateTransitional.getAssertionConsumerServiceUri(),
                            userAccountCreatedStateTransitional.getSessionId(),
                            userAccountCreatedStateTransitional.getIdentityProviderEntityId(),
                            userAccountCreatedStateTransitional.getMatchingServiceAssertion(),
                            userAccountCreatedStateTransitional.getRelayState().orNull(),
                            userAccountCreatedStateTransitional.getLevelOfAssurance(),
                            userAccountCreatedStateTransitional.isRegistering(),
                            userAccountCreatedStateTransitional.getTransactionSupportsEidas()
                    ),
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class)
                );

            case USER_ACCOUNT_CREATED:
                return new UserAccountCreatedStateController(
                        (UserAccountCreatedState) state,
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class)
                );

            // Deprecated
            case AWAITING_CYCLE3_DATA_TRANSITIONAL:
                final AwaitingCycle3DataStateTransitional awaitingCycle3DataStateTransitional = (AwaitingCycle3DataStateTransitional) state;
                return new AwaitingCycle3DataStateController(
                    new AwaitingCycle3DataState(
                            awaitingCycle3DataStateTransitional.getRequestId(),
                            awaitingCycle3DataStateTransitional.getIdentityProviderEntityId(),
                            awaitingCycle3DataStateTransitional.getSessionExpiryTimestamp(),
                            awaitingCycle3DataStateTransitional.getRequestIssuerEntityId(),
                            awaitingCycle3DataStateTransitional.getEncryptedMatchingDatasetAssertion(),
                            awaitingCycle3DataStateTransitional.getAuthnStatementAssertion(),
                            awaitingCycle3DataStateTransitional.getRelayState().orNull(),
                            awaitingCycle3DataStateTransitional.getAssertionConsumerServiceUri(),
                            awaitingCycle3DataStateTransitional.getMatchingServiceEntityId(),
                            awaitingCycle3DataStateTransitional.getSessionId(),
                            awaitingCycle3DataStateTransitional.getPersistentId(),
                            awaitingCycle3DataStateTransitional.getLevelOfAssurance(),
                            awaitingCycle3DataStateTransitional.isRegistering(),
                            awaitingCycle3DataStateTransitional.getTransactionSupportsEidas()
                    ),
                    injector.getInstance(EventSinkHubEventLogger.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(PolicyConfiguration.class),
                    injector.getInstance(AssertionRestrictionsFactory.class),
                    injector.getInstance(MatchingServiceConfigProxy.class)
                );

            case AWAITING_CYCLE3_DATA:
                return new AwaitingCycle3DataStateController(
                        (AwaitingCycle3DataState) state,
                        injector.getInstance(EventSinkHubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(PolicyConfiguration.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(MatchingServiceConfigProxy.class)
                );

            case EIDAS_AWAITING_CYCLE3_DATA:
                return new EidasAwaitingCycle3DataStateController(
                    (EidasAwaitingCycle3DataState) state,
                    injector.getInstance(EventSinkHubEventLogger.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(PolicyConfiguration.class),
                    injector.getInstance(AssertionRestrictionsFactory.class),
                    injector.getInstance(MatchingServiceConfigProxy.class)
                );

            // Deprecated
            case CYCLE3_MATCH_REQUEST_SENT_TRANSITIONAL:
                return new Cycle3MatchRequestSentStateController(
                    (Cycle3MatchRequestSentStateTransitional) state,
                    injector.getInstance(EventSinkHubEventLogger.class),
                    stateTransitionAction,
                    injector.getInstance(PolicyConfiguration.class),
                    new LevelOfAssuranceValidator(),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(MatchingServiceConfigProxy.class),
                    injector.getInstance(AssertionRestrictionsFactory.class),
                    injector.getInstance(AttributeQueryService.class)
                );

            case CYCLE3_MATCH_REQUEST_SENT:
                final Cycle3MatchRequestSentState cycle3MatchRequestSentState = (Cycle3MatchRequestSentState) state;
                return new Cycle3MatchRequestSentStateController(
                        new Cycle3MatchRequestSentStateTransitional(
                                cycle3MatchRequestSentState.getRequestId(),
                                cycle3MatchRequestSentState.getRequestIssuerEntityId(),
                                cycle3MatchRequestSentState.getSessionExpiryTimestamp(),
                                cycle3MatchRequestSentState.getAssertionConsumerServiceUri(),
                                cycle3MatchRequestSentState.getSessionId(),
                                cycle3MatchRequestSentState.getTransactionSupportsEidas(),
                                cycle3MatchRequestSentState.getIdentityProviderEntityId(),
                                cycle3MatchRequestSentState.getRelayState(),
                                cycle3MatchRequestSentState.getIdpLevelOfAssurance(),
                                cycle3MatchRequestSentState.isRegistering(),
                                cycle3MatchRequestSentState.getMatchingServiceAdapterEntityId(),
                                cycle3MatchRequestSentState.getEncryptedMatchingDatasetAssertion(),
                                cycle3MatchRequestSentState.getAuthnStatementAssertion(),
                                cycle3MatchRequestSentState.getPersistentId(),
                                cycle3MatchRequestSentState.getRequestSentTime()
                        ),
                        injector.getInstance(EventSinkHubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(MatchingServiceConfigProxy.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(AttributeQueryService.class)
                );

            case TIMEOUT:
                return new TimeoutStateController(
                    (TimeoutState) state,
                    injector.getInstance(ResponseFromHubFactory.class)
                );
            case MATCHING_SERVICE_REQUEST_ERROR:
                return new MatchingServiceRequestErrorStateController(
                    (MatchingServiceRequestErrorState) state,
                    injector.getInstance(ResponseFromHubFactory.class)
                );

            // Deprecated
            case USER_ACCOUNT_CREATION_REQUEST_SENT_TRANSITIONAL:
                return new UserAccountCreationRequestSentStateController(
                    (UserAccountCreationRequestSentStateTransitional) state,
                    stateTransitionAction,
                    injector.getInstance(EventSinkHubEventLogger.class),
                    injector.getInstance(PolicyConfiguration.class),
                    new LevelOfAssuranceValidator(),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(AttributeQueryService.class)
                );

            case USER_ACCOUNT_CREATION_REQUEST_SENT:
                final UserAccountCreationRequestSentState userAccountCreationRequestSentState = (UserAccountCreationRequestSentState) state;
                return new UserAccountCreationRequestSentStateController(
                        new UserAccountCreationRequestSentStateTransitional(
                            userAccountCreationRequestSentState.getRequestId(),
                            userAccountCreationRequestSentState.getRequestIssuerEntityId(),
                            userAccountCreationRequestSentState.getSessionExpiryTimestamp(),
                            userAccountCreationRequestSentState.getAssertionConsumerServiceUri(),
                            userAccountCreationRequestSentState.getSessionId(),
                            userAccountCreationRequestSentState.getTransactionSupportsEidas(),
                            userAccountCreationRequestSentState.getIdentityProviderEntityId(),
                            userAccountCreationRequestSentState.getRelayState(),
                            userAccountCreationRequestSentState.getIdpLevelOfAssurance(),
                            userAccountCreationRequestSentState.isRegistering(),
                            userAccountCreationRequestSentState.getMatchingServiceAdapterEntityId(),
                            userAccountCreationRequestSentState.getRequestSentTime()
                        ),
                        stateTransitionAction,
                        injector.getInstance(EventSinkHubEventLogger.class),
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(AttributeQueryService.class)
                );

            // Deprecated
            case AUTHN_FAILED_ERROR_TRANSITIONAL:
                final AuthnFailedErrorStateTransitional authnFailedErrorStateTransitional = (AuthnFailedErrorStateTransitional) state;
                return new AuthnFailedErrorStateController(
                    new AuthnFailedErrorState(
                            authnFailedErrorStateTransitional.getRequestId(),
                            authnFailedErrorStateTransitional.getRequestIssuerEntityId(),
                            authnFailedErrorStateTransitional.getSessionExpiryTimestamp(),
                            authnFailedErrorStateTransitional.getAssertionConsumerServiceUri(),
                            authnFailedErrorStateTransitional.getRelayState().orNull(),
                            authnFailedErrorStateTransitional.getSessionId(),
                            authnFailedErrorStateTransitional.getIdpEntityId(),
                            authnFailedErrorStateTransitional.getForceAuthentication().orNull(),
                            authnFailedErrorStateTransitional.getTransactionSupportsEidas()
                    ),
                    injector.getInstance(ResponseFromHubFactory.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(EventSinkHubEventLogger.class)
                );

            case AUTHN_FAILED_ERROR:
                return new AuthnFailedErrorStateController(
                        (AuthnFailedErrorState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(EventSinkHubEventLogger.class)
                );

            // Deprecated
            case FRAUD_EVENT_DETECTED_TRANSITIONAL:
                final FraudEventDetectedStateTransitional fraudEventDetectedStateTransitional = (FraudEventDetectedStateTransitional) state;
                return new FraudEventDetectedStateController(
                    new FraudEventDetectedState(
                            fraudEventDetectedStateTransitional.getRequestId(),
                            fraudEventDetectedStateTransitional.getRequestIssuerEntityId(),
                            fraudEventDetectedStateTransitional.getSessionExpiryTimestamp(),
                            fraudEventDetectedStateTransitional.getAssertionConsumerServiceUri(),
                            fraudEventDetectedStateTransitional.getRelayState().orNull(),
                            fraudEventDetectedStateTransitional.getSessionId(),
                            fraudEventDetectedStateTransitional.getIdpEntityId(),
                            fraudEventDetectedStateTransitional.getForceAuthentication().orNull(),
                            fraudEventDetectedStateTransitional.getTransactionSupportsEidas()
                    ),
                    injector.getInstance(ResponseFromHubFactory.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(EventSinkHubEventLogger.class)
                );

            case FRAUD_EVENT_DETECTED:
                return new FraudEventDetectedStateController(
                        (FraudEventDetectedState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(EventSinkHubEventLogger.class)
                );

            // Deprecated
            case REQUESTER_ERROR_TRANSITIONAL:
                final RequesterErrorStateTransitional requesterErrorStateTransitional = (RequesterErrorStateTransitional) state;
                return new RequesterErrorStateController(
                    new RequesterErrorState(
                            requesterErrorStateTransitional.getRequestId(),
                            requesterErrorStateTransitional.getRequestIssuerEntityId(),
                            requesterErrorStateTransitional.getSessionExpiryTimestamp(),
                            requesterErrorStateTransitional.getAssertionConsumerServiceUri(),
                            requesterErrorStateTransitional.getRelayState().orNull(),
                            requesterErrorStateTransitional.getSessionId(),
                            requesterErrorStateTransitional.getForceAuthentication().orNull(),
                            requesterErrorStateTransitional.getTransactionSupportsEidas()
                    ),
                    injector.getInstance(ResponseFromHubFactory.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(EventSinkHubEventLogger.class)
                );

            case REQUESTER_ERROR:
                return new RequesterErrorStateController(
                        (RequesterErrorState) state,
                        injector.getInstance(ResponseFromHubFactory.class),
                        stateTransitionAction,
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(EventSinkHubEventLogger.class)
                );

            case CYCLE_3_DATA_INPUT_CANCELLED:
                return new Cycle3DataInputCancelledStateController(
                    (Cycle3DataInputCancelledState) state,
                    injector.getInstance(ResponseFromHubFactory.class)
                );
            case USER_ACCOUNT_CREATION_FAILED:
                return new UserAccountCreationFailedStateController(
                    (UserAccountCreationFailedState) state,
                    injector.getInstance(ResponseFromHubFactory.class)
                );
            default:
                throw new IllegalStateException(format("Invalid state controller class for {0}", policyState));
        }
    }
}
