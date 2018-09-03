package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.List;

public class EidasCycle3MatchRequestSentStateController extends EidasMatchRequestSentStateController<EidasCycle3MatchRequestSentState> {

    public EidasCycle3MatchRequestSentStateController(
            final EidasCycle3MatchRequestSentState state,
            final HubEventLogger hubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final PolicyConfiguration policyConfiguration,
            final LevelOfAssuranceValidator validator,
            final ResponseFromHubFactory responseFromHubFactory,
            final AttributeQueryService attributeQueryService,
            final TransactionsConfigProxy transactionsConfigProxy,
            final MatchingServiceConfigProxy matchingServiceConfigProxy) {

        super(
                state,
                stateTransitionAction,
                hubEventLogger,
                policyConfiguration,
                validator,
                responseFromHubFactory,
                attributeQueryService,
                transactionsConfigProxy,
                matchingServiceConfigProxy);
    }

    @Override
    protected void transitionToNextStateForMatchResponse(String matchingServiceAssertion) {
        hubEventLogger.logCycle3SuccessfulMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId()
        );

        stateTransitionAction.transitionTo(createSuccessfulMatchState(matchingServiceAssertion));
    }

    @Override
    protected void transitionToNextStateForNoMatchResponse() {
        List<UserAccountCreationAttribute> userAccountCreationAttributes = transactionsConfigProxy.getUserAccountCreationAttributes(state.getRequestIssuerEntityId());
        if (!userAccountCreationAttributes.isEmpty()) {
            EidasAttributeQueryRequestDto attributeQueryRequestDto = createAttributeQuery(userAccountCreationAttributes, state.getEncryptedIdentityAssertion(), state.getPersistentId());
            transitionToUserAccountCreationRequestSentState(attributeQueryRequestDto);
            return;
        }

        hubEventLogger.logCycle3NoMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId());


        stateTransitionAction.transitionTo(createNoMatchState());
    }
}
