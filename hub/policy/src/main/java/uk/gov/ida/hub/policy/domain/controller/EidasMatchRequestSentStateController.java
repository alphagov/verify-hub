package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.EidasMatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

public abstract class EidasMatchRequestSentStateController<T extends EidasMatchRequestSentState> extends AbstractMatchRequestSentStateController<T, EidasSuccessfulMatchState> {

    public EidasMatchRequestSentStateController(
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

}
