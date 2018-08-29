package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.List;

public class Cycle3MatchRequestSentStateController extends MatchRequestSentStateController<Cycle3MatchRequestSentState> {

    private final AssertionRestrictionsFactory assertionRestrictionFactory;

    public Cycle3MatchRequestSentStateController(
            final Cycle3MatchRequestSentState state,
            final HubEventLogger hubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final PolicyConfiguration policyConfiguration,
            final LevelOfAssuranceValidator validator,
            final ResponseFromHubFactory responseFromHubFactory,
            final TransactionsConfigProxy transactionsConfigProxy,
            final MatchingServiceConfigProxy matchingServiceConfigProxy,
            final AssertionRestrictionsFactory assertionRestrictionFactory,
            final AttributeQueryService attributeQueryService) {

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

        this.assertionRestrictionFactory = assertionRestrictionFactory;
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
        if(!userAccountCreationAttributes.isEmpty()) {
            AttributeQueryRequestDto attributeQueryRequestDto = createAttributeQuery(
                    userAccountCreationAttributes,
                    state.getEncryptedMatchingDatasetAssertion(),
                    state.getAuthnStatementAssertion(),
                    state.getPersistentId(),
                    assertionRestrictionFactory.getAssertionExpiry());

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
