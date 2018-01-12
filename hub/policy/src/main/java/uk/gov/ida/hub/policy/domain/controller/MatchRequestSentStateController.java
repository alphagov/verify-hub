package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

public abstract class MatchRequestSentStateController<T extends MatchRequestSentState> extends MatchRequestSentStateControllerBase<T, SuccessfulMatchState> {

    public MatchRequestSentStateController(
            final T state,
            final StateTransitionAction stateTransitionAction,
            final EventSinkHubEventLogger eventSinkHubEventLogger,
            final PolicyConfiguration policyConfiguration,
            final LevelOfAssuranceValidator validator,
            final ResponseFromHubFactory responseFromHubFactory,
            final AttributeQueryService attributeQueryService) {

        super(
                state,
                stateTransitionAction,
                eventSinkHubEventLogger,
                policyConfiguration,
                validator,
                responseFromHubFactory,
                attributeQueryService
        );
    }

    protected State handleUserAccountCreationRequestAndGenerateState(AttributeQueryRequestDto attributeQueryRequestDto) {
        eventSinkHubEventLogger.logMatchingServiceUserAccountCreationRequestSentEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId());

        attributeQueryService.sendAttributeQueryRequest(state.getSessionId(), attributeQueryRequestDto);

        return new UserAccountCreationRequestSentState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getTransactionSupportsEidas(),
                state.getIdentityProviderEntityId(),
                state.getRelayState(),
                state.getIdpLevelOfAssurance(),
                state.isRegistering(),
                state.getMatchingServiceAdapterEntityId()
        );
    }
}
