package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;


public class UserAccountCreationRequestSentStateController extends MatchRequestSentStateController<UserAccountCreationRequestSentState> {
    protected final UserAccountCreationRequestSentState state;
    protected final EventSinkHubEventLogger eventSinkHubEventLogger;
    private final LevelOfAssuranceValidator validator;

    public UserAccountCreationRequestSentStateController(
            final UserAccountCreationRequestSentState state,
            final StateTransitionAction stateTransitionAction,
            final EventSinkHubEventLogger eventSinkHubEventLogger,
            final PolicyConfiguration policyConfiguration,
            final LevelOfAssuranceValidator validator,
            final ResponseFromHubFactory responseFromHubFactory,
            final AttributeQueryService attributeQueryService) {
        super(state, stateTransitionAction, eventSinkHubEventLogger, policyConfiguration, validator, responseFromHubFactory, attributeQueryService);
        this.state = state;
        this.eventSinkHubEventLogger = eventSinkHubEventLogger;
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
    protected State getNextStateForUserAccountCreated(UserAccountCreatedFromMatchingService responseFromMatchingService) {
        eventSinkHubEventLogger.logUserAccountCreatedEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());

        validator.validate(responseFromMatchingService.getLevelOfAssurance(), state.getIdpLevelOfAssurance());
        return new UserAccountCreatedState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdentityProviderEntityId(),
                responseFromMatchingService.getMatchingServiceAssertion(),
                state.getRelayState(),
                state.getIdpLevelOfAssurance(),
                state.getTransactionSupportsEidas());
    }

    public UserAccountCreationFailedState getNextStateForUserAccountCreationFailed() {
        eventSinkHubEventLogger.logUserAccountCreationFailedEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());
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
