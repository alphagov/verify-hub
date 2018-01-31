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
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateFactory;
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
            case SESSION_STARTED:
                return new SessionStartedStateController(
                    (SessionStartedStateTransitional) state,
                    injector.getInstance(EventSinkHubEventLogger.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class));

            // Deprecated
            case SESSION_STARTED_OLD:
                SessionStartedState sessionStartedState = (SessionStartedState) state;
                return new SessionStartedStateController(
                        new SessionStartedStateTransitional(
                                sessionStartedState.getRequestId(),
                                sessionStartedState.getRelayState(),
                                sessionStartedState.getRequestIssuerEntityId(),
                                sessionStartedState.getAssertionConsumerServiceUri(),
                                sessionStartedState.getForceAuthentication(),
                                sessionStartedState.getSessionExpiryTimestamp(),
                                sessionStartedState.getSessionId(),
                                sessionStartedState.getTransactionSupportsEidas()
                        ),
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

            case IDP_SELECTED:
                return new IdpSelectedStateController(
                    (IdpSelectedStateTransitional) state,
                    injector.getInstance(SessionStartedStateFactory.class),
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
            case IDP_SELECTED_OLD:
                IdpSelectedState idpSelectedState = (IdpSelectedState) state;
                return new IdpSelectedStateController(
                        new IdpSelectedStateTransitional(
                                idpSelectedState.getRequestId(),
                                idpSelectedState.getIdpEntityId(),
                                idpSelectedState.getMatchingServiceEntityId(),
                                idpSelectedState.getLevelsOfAssurance(),
                                idpSelectedState.getUseExactComparisonType(),
                                idpSelectedState.getForceAuthentication(),
                                idpSelectedState.getAssertionConsumerServiceUri(),
                                idpSelectedState.getRequestIssuerEntityId(),
                                idpSelectedState.getRelayState(),
                                idpSelectedState.getSessionExpiryTimestamp(),
                                idpSelectedState.registering(),
                                null,
                                idpSelectedState.getSessionId(),
                                idpSelectedState.getAvailableIdentityProviderEntityIds(),
                                idpSelectedState.getTransactionSupportsEidas()
                        ),
                        injector.getInstance(SessionStartedStateFactory.class),
                        injector.getInstance(EventSinkHubEventLogger.class),
                        stateTransitionAction,
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(PolicyConfiguration.class),
                        injector.getInstance(AssertionRestrictionsFactory.class),
                        injector.getInstance(MatchingServiceConfigProxy.class)
                );

            case CYCLE_0_AND_1_MATCH_REQUEST_SENT:
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

            // Deprecated
            case CYCLE_0_AND_1_MATCH_REQUEST_SENT_OLD:
                final Cycle0And1MatchRequestSentState cycle0And1MatchRequestSentState = (Cycle0And1MatchRequestSentState) state;
                return new Cycle0And1MatchRequestSentStateController(
                        new Cycle0And1MatchRequestSentStateTransitional(
                                cycle0And1MatchRequestSentState.getRequestId(),
                                cycle0And1MatchRequestSentState.getRequestIssuerEntityId(),
                                cycle0And1MatchRequestSentState.getSessionExpiryTimestamp(),
                                cycle0And1MatchRequestSentState.getAssertionConsumerServiceUri(),
                                cycle0And1MatchRequestSentState.getSessionId(),
                                cycle0And1MatchRequestSentState.getTransactionSupportsEidas(),
                                false,
                                cycle0And1MatchRequestSentState.getIdentityProviderEntityId(),
                                cycle0And1MatchRequestSentState.getRelayState(),
                                cycle0And1MatchRequestSentState.getIdpLevelOfAssurance(),
                                cycle0And1MatchRequestSentState.getMatchingServiceAdapterEntityId(),
                                cycle0And1MatchRequestSentState.getEncryptedMatchingDatasetAssertion(),
                                cycle0And1MatchRequestSentState.getAuthnStatementAssertion(),
                                cycle0And1MatchRequestSentState.getPersistentId()
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
            case SUCCESSFUL_MATCH:
                return new SuccessfulMatchStateController(
                    (SuccessfulMatchStateTransitional) state,
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class)
                );

            // Deprecated
            case SUCCESSFUL_MATCH_OLD:
                final SuccessfulMatchState successfulMatchState = (SuccessfulMatchState) state;
                return new SuccessfulMatchStateController(
                        new SuccessfulMatchStateTransitional(
                                successfulMatchState.getRequestId(),
                                successfulMatchState.getSessionExpiryTimestamp(),
                                successfulMatchState.getIdentityProviderEntityId(),
                                successfulMatchState.getMatchingServiceAssertion(),
                                successfulMatchState.getRelayState(),
                                successfulMatchState.getRequestIssuerEntityId(),
                                successfulMatchState.getAssertionConsumerServiceUri(),
                                successfulMatchState.getSessionId(),
                                successfulMatchState.getLevelOfAssurance(),
                                false,
                                successfulMatchState.getTransactionSupportsEidas()
                        ),
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
            case USER_ACCOUNT_CREATED:
                return new UserAccountCreatedStateController(
                    (UserAccountCreatedStateTransitional) state,
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class)
                );


            // Deprecated
            case USER_ACCOUNT_CREATED_OLD:
                final UserAccountCreatedState userAccountCreatedState = (UserAccountCreatedState) state;
                return new UserAccountCreatedStateController(
                        new UserAccountCreatedStateTransitional(
                                userAccountCreatedState.getRequestId(),
                                userAccountCreatedState.getRequestIssuerEntityId(),
                                userAccountCreatedState.getSessionExpiryTimestamp(),
                                userAccountCreatedState.getAssertionConsumerServiceUri(),
                                userAccountCreatedState.getSessionId(),
                                userAccountCreatedState.getIdentityProviderEntityId(),
                                userAccountCreatedState.getMatchingServiceAssertion(),
                                userAccountCreatedState.getRelayState(),
                                userAccountCreatedState.getLevelOfAssurance(),
                                false,
                                userAccountCreatedState.getTransactionSupportsEidas()
                        ),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(ResponseFromHubFactory.class)
                );

            case AWAITING_CYCLE3_DATA:
                return new AwaitingCycle3DataStateController(
                    (AwaitingCycle3DataStateTransitional) state,
                    injector.getInstance(EventSinkHubEventLogger.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(PolicyConfiguration.class),
                    injector.getInstance(AssertionRestrictionsFactory.class),
                    injector.getInstance(MatchingServiceConfigProxy.class)
                );

            // Deprecated
            case AWAITING_CYCLE3_DATA_OLD:
                final AwaitingCycle3DataState awaitingCycle3DataState = (AwaitingCycle3DataState) state;
                return new AwaitingCycle3DataStateController(
                        new AwaitingCycle3DataStateTransitional(
                                awaitingCycle3DataState.getRequestId(),
                                awaitingCycle3DataState.getIdentityProviderEntityId(),
                                awaitingCycle3DataState.getSessionExpiryTimestamp(),
                                awaitingCycle3DataState.getRequestIssuerEntityId(),
                                awaitingCycle3DataState.getEncryptedMatchingDatasetAssertion(),
                                awaitingCycle3DataState.getAuthnStatementAssertion(),
                                awaitingCycle3DataState.getRelayState(),
                                awaitingCycle3DataState.getAssertionConsumerServiceUri(),
                                awaitingCycle3DataState.getMatchingServiceEntityId(),
                                awaitingCycle3DataState.getSessionId(),
                                awaitingCycle3DataState.getPersistentId(),
                                awaitingCycle3DataState.getLevelOfAssurance(),
                                false,
                                awaitingCycle3DataState.getTransactionSupportsEidas()
                        ),
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
            case CYCLE3_MATCH_REQUEST_SENT:
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

            // Deprecated
            case CYCLE3_MATCH_REQUEST_SENT_OLD:
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
                                false,
                                cycle3MatchRequestSentState.getMatchingServiceAdapterEntityId(),
                                cycle3MatchRequestSentState.getEncryptedMatchingDatasetAssertion(),
                                cycle3MatchRequestSentState.getAuthnStatementAssertion(),
                                cycle3MatchRequestSentState.getPersistentId()
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
            case USER_ACCOUNT_CREATION_REQUEST_SENT:
                return new UserAccountCreationRequestSentStateController(
                    (UserAccountCreationRequestSentStateTransitional) state,
                    stateTransitionAction,
                    injector.getInstance(EventSinkHubEventLogger.class),
                    injector.getInstance(PolicyConfiguration.class),
                    new LevelOfAssuranceValidator(),
                    injector.getInstance(ResponseFromHubFactory.class),
                    injector.getInstance(AttributeQueryService.class)
                );

            // Deprecated
            case USER_ACCOUNT_CREATION_REQUEST_SENT_OLD:
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
                                false,
                                userAccountCreationRequestSentState.getMatchingServiceAdapterEntityId()
                        ),
                        stateTransitionAction,
                        injector.getInstance(EventSinkHubEventLogger.class),
                        injector.getInstance(PolicyConfiguration.class),
                        new LevelOfAssuranceValidator(),
                        injector.getInstance(ResponseFromHubFactory.class),
                        injector.getInstance(AttributeQueryService.class)
                );

            case AUTHN_FAILED_ERROR:
                return new AuthnFailedErrorStateController(
                    (AuthnFailedErrorStateTransitional) state,
                    injector.getInstance(ResponseFromHubFactory.class),
                    stateTransitionAction,
                    injector.getInstance(SessionStartedStateFactory.class),
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(EventSinkHubEventLogger.class)
                );


            // Deprecated
            case AUTHN_FAILED_ERROR_OLD:
                final AuthnFailedErrorState authnFailedErrorState = (AuthnFailedErrorState) state;
                return new AuthnFailedErrorStateController(
                        new AuthnFailedErrorStateTransitional(
                                authnFailedErrorState.getRequestId(),
                                authnFailedErrorState.getRequestIssuerEntityId(),
                                authnFailedErrorState.getSessionExpiryTimestamp(),
                                authnFailedErrorState.getAssertionConsumerServiceUri(),
                                authnFailedErrorState.getRelayState(),
                                authnFailedErrorState.getSessionId(),
                                authnFailedErrorState.getIdpEntityId(),
                                authnFailedErrorState.getForceAuthentication(),
                                authnFailedErrorState.getTransactionSupportsEidas()
                        ),
                        injector.getInstance(ResponseFromHubFactory.class),
                        stateTransitionAction,
                        injector.getInstance(SessionStartedStateFactory.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(EventSinkHubEventLogger.class)
                );

            case FRAUD_EVENT_DETECTED:
                return new FraudEventDetectedStateController(
                    (FraudEventDetectedStateTransitional) state,
                    injector.getInstance(ResponseFromHubFactory.class),
                    stateTransitionAction,
                    injector.getInstance(SessionStartedStateFactory.class),
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(EventSinkHubEventLogger.class)
                );

            // Deprecated
            case FRAUD_EVENT_DETECTED_OLD:
                final FraudEventDetectedState fraudEventDetectedState = (FraudEventDetectedState) state;
                return new FraudEventDetectedStateController(
                        new FraudEventDetectedStateTransitional(
                                fraudEventDetectedState.getRequestId(),
                                fraudEventDetectedState.getRequestIssuerEntityId(),
                                fraudEventDetectedState.getSessionExpiryTimestamp(),
                                fraudEventDetectedState.getAssertionConsumerServiceUri(),
                                fraudEventDetectedState.getRelayState(),
                                fraudEventDetectedState.getSessionId(),
                                fraudEventDetectedState.getIdpEntityId(),
                                fraudEventDetectedState.getForceAuthentication(),
                                fraudEventDetectedState.getTransactionSupportsEidas()
                        ),
                        injector.getInstance(ResponseFromHubFactory.class),
                        stateTransitionAction,
                        injector.getInstance(SessionStartedStateFactory.class),
                        injector.getInstance(TransactionsConfigProxy.class),
                        injector.getInstance(IdentityProvidersConfigProxy.class),
                        injector.getInstance(EventSinkHubEventLogger.class)
                );

            case REQUESTER_ERROR:
                return new RequesterErrorStateController(
                    (RequesterErrorStateTransitional) state,
                    injector.getInstance(ResponseFromHubFactory.class),
                    stateTransitionAction,
                    injector.getInstance(TransactionsConfigProxy.class),
                    injector.getInstance(IdentityProvidersConfigProxy.class),
                    injector.getInstance(EventSinkHubEventLogger.class)
                );

            // Deprecated
            case REQUESTER_ERROR_OLD:
                final RequesterErrorState requesterErrorState = (RequesterErrorState) state;
                return new RequesterErrorStateController(
                        new RequesterErrorStateTransitional(
                                requesterErrorState.getRequestId(),
                                requesterErrorState.getRequestIssuerEntityId(),
                                requesterErrorState.getSessionExpiryTimestamp(),
                                requesterErrorState.getAssertionConsumerServiceUri(),
                                requesterErrorState.getRelayState(),
                                requesterErrorState.getSessionId(),
                                requesterErrorState.getForceAuthentication(),
                                requesterErrorState.getTransactionSupportsEidas()
                        ),
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
