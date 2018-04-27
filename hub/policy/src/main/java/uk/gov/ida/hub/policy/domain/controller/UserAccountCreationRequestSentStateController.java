package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;


public class UserAccountCreationRequestSentStateController extends MatchRequestSentStateController<UserAccountCreationRequestSentState> {
    protected final UserAccountCreationRequestSentState state;
    protected final HubEventLogger hubEventLogger;
    private final LevelOfAssuranceValidator validator;

    public UserAccountCreationRequestSentStateController(
            final UserAccountCreationRequestSentState state,
            final StateTransitionAction stateTransitionAction,
            final HubEventLogger hubEventLogger,
            final PolicyConfiguration policyConfiguration,
            final LevelOfAssuranceValidator validator,
            final ResponseFromHubFactory responseFromHubFactory,
            final AttributeQueryService attributeQueryService,
            final TransactionsConfigProxy transactionsConfigProxy,
            final MatchingServiceConfigProxy matchingServiceConfigProxy) {
        super(state, stateTransitionAction, hubEventLogger, policyConfiguration, validator, responseFromHubFactory, attributeQueryService, transactionsConfigProxy, matchingServiceConfigProxy);
        this.state = state;
        this.hubEventLogger = hubEventLogger;
        this.validator = validator;
    }

    @Override
    protected State getNextStateForMatch(MatchFromMatchingService responseFromMatchingService) {
        return null;
    }

    @Override
    protected State getNextStateForNoMatch() {
        return null;
    }

    @Override
    protected SuccessfulMatchState createSuccessfulMatchState(String matchingServiceAssertion, String requestIssuerId) {
        return new SuccessfulMatchState(
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdentityProviderEntityId(),
                matchingServiceAssertion,
                state.getRelayState().orNull(),
                requestIssuerId,
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdpLevelOfAssurance(),
                state.isRegistering(),
                state.getTransactionSupportsEidas()
        );
    }

    @Override
    protected State getNextStateForUserAccountCreated(UserAccountCreatedFromMatchingService responseFromMatchingService) {
        hubEventLogger.logUserAccountCreatedEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());

        validator.validate(responseFromMatchingService.getLevelOfAssurance(), state.getIdpLevelOfAssurance());
        return new UserAccountCreatedState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdentityProviderEntityId(),
                responseFromMatchingService.getMatchingServiceAssertion(),
                state.getRelayState().orNull(),
                state.getIdpLevelOfAssurance(),
                state.isRegistering(),
                state.getTransactionSupportsEidas());
    }

    public UserAccountCreationFailedState getNextStateForUserAccountCreationFailed() {
        hubEventLogger.logUserAccountCreationFailedEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());
        return new UserAccountCreationFailedState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getSessionId(),
                state.getTransactionSupportsEidas()
        );
    }
}
