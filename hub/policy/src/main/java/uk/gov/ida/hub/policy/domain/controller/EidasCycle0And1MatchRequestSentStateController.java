package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.List;
import java.util.Optional;

public class EidasCycle0And1MatchRequestSentStateController extends EidasMatchRequestSentStateController<EidasCycle0And1MatchRequestSentState> {

    public EidasCycle0And1MatchRequestSentStateController(
            final EidasCycle0And1MatchRequestSentState state,
            final StateTransitionAction stateTransitionAction,
            final HubEventLogger hubEventLogger,
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
            hubEventLogger.logWaitingForCycle3AttributesEvent(
                    state.getSessionId(),
                    state.getRequestIssuerEntityId(),
                    state.getRequestId(),
                    state.getSessionExpiryTimestamp());

            stateTransitionAction.transitionTo(getAwaitingCycle3DataState());
            return;
        }

        List<UserAccountCreationAttribute> userAccountCreationAttributes = transactionsConfigProxy.getUserAccountCreationAttributes(state.getRequestIssuerEntityId());
        if (!userAccountCreationAttributes.isEmpty()) {
            EidasAttributeQueryRequestDto attributeQueryRequestDto = createAttributeQuery(
                    userAccountCreationAttributes,
                    state.getEncryptedIdentityAssertion(),
                    state.getPersistentId(),
                    state.getCountrySignedResponseContainer());
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

    private EidasAwaitingCycle3DataState getAwaitingCycle3DataState() {
        return new EidasAwaitingCycle3DataState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getTransactionSupportsEidas(),
                state.getIdentityProviderEntityId(),
                state.getMatchingServiceAdapterEntityId(),
                state.getRelayState().orElse(null),
                state.getPersistentId(),
                state.getIdpLevelOfAssurance(),
                state.getEncryptedIdentityAssertion(),
                state.getForceAuthentication().orElse(null),
                state.getCountrySignedResponseContainer().orElse(null));
    }
}
