package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.List;
import java.util.Optional;

public class Cycle0And1MatchRequestSentStateController extends MatchRequestSentStateController<Cycle0And1MatchRequestSentState> {

    private final AssertionRestrictionsFactory assertionRestrictionFactory;

    public Cycle0And1MatchRequestSentStateController(
            final Cycle0And1MatchRequestSentState state,
            final HubEventLogger hubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final PolicyConfiguration policyConfiguration,
            final LevelOfAssuranceValidator validator,
            final TransactionsConfigProxy transactionsConfigProxy,
            final ResponseFromHubFactory responseFromHubFactory,
            final AssertionRestrictionsFactory assertionRestrictionFactory,
            final MatchingServiceConfigProxy matchingServiceConfigProxy,
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
        hubEventLogger.logCycle01SuccessfulMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getRequestId(),
                state.getSessionExpiryTimestamp());

        stateTransitionAction.transitionTo(createSuccessfulMatchState(matchingServiceAssertion));
    }

    @Override
    protected void transitionToNextStateForNoMatchResponse() {
        Optional<String> selfAssertedAttributeName = transactionsConfigProxy.getMatchingProcess(state.getRequestIssuerEntityId()).getAttributeName();
        if (selfAssertedAttributeName.isPresent()) {
            hubEventLogger.logWaitingForCycle3AttributesEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());
            stateTransitionAction.transitionTo(createAwaitingCycle3DataState());
            return;
        }

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

        hubEventLogger.logCycle01NoMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getRequestId(),
                state.getSessionExpiryTimestamp());

        stateTransitionAction.transitionTo(createNoMatchState());
    }

    private AwaitingCycle3DataState createAwaitingCycle3DataState() {
        return new AwaitingCycle3DataState(
                state.getRequestId(),
                state.getIdentityProviderEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestIssuerEntityId(),
                state.getEncryptedMatchingDatasetAssertion(),
                state.getAuthnStatementAssertion(),
                state.getRelayState().orElse(null),
                state.getAssertionConsumerServiceUri(),
                state.getMatchingServiceAdapterEntityId(),
                state.getSessionId(),
                state.getPersistentId(),
                state.getIdpLevelOfAssurance(),
                state.isRegistering(),
                state.getTransactionSupportsEidas());
    }
}
