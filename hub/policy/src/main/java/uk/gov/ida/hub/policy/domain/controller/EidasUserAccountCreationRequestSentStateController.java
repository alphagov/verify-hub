package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

public class EidasUserAccountCreationRequestSentStateController extends EidasMatchRequestSentStateController<EidasUserAccountCreationRequestSentState> {

    public EidasUserAccountCreationRequestSentStateController(
            final EidasUserAccountCreationRequestSentState state,
            final StateTransitionAction stateTransitionAction,
            final HubEventLogger hubEventLogger,
            final PolicyConfiguration policyConfiguration,
            final LevelOfAssuranceValidator validator,
            final ResponseFromHubFactory responseFromHubFactory,
            final AttributeQueryService attributeQueryService,
            final TransactionsConfigProxy transactionsConfigProxy,
            final MatchingServiceConfigProxy matchingServiceConfigProxy) {

        super(state, stateTransitionAction, hubEventLogger, policyConfiguration, validator, responseFromHubFactory, attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
    }

    @Override
    protected void transitionToNextStateForUserAccountCreatedResponse(UserAccountCreatedFromMatchingService responseFromMatchingService) {
        final UserAccountCreatedState userAccountCreatedState = new UserAccountCreatedState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdentityProviderEntityId(),
                responseFromMatchingService.getMatchingServiceAssertion(),
                state.getRelayState().orElse(null),
                state.getIdpLevelOfAssurance(),
                false,
                state.getTransactionSupportsEidas());

        stateTransitionAction.transitionTo(userAccountCreatedState);
    }

    @Override
    public void transitionToNextStateForUserAccountCreationFailedResponse() {
        final EidasUserAccountCreationFailedState eidasUserAccountCreationFailedState = new EidasUserAccountCreationFailedState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState().orElse(null),
                state.getSessionId(),
                state.getForceAuthentication().orElse(null)
        );

        stateTransitionAction.transitionTo(eidasUserAccountCreationFailedState);
    }
}
