package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AbstractAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.MatchingProcess;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.AbstractAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

public abstract class AbstractAwaitingCycle3DataStateController<S extends AbstractAttributeQueryRequestDto, T extends AbstractAwaitingCycle3DataState> implements StateController, ResponseProcessingStateController, ErrorResponsePreparedStateController {

    private final T state;
    private final HubEventLogger hubEventLogger;
    private final StateTransitionAction stateTransitionAction;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final ResponseFromHubFactory responseFromHubFactory;
    private final AssertionRestrictionsFactory assertionRestrictionsFactory;
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final PolicyConfiguration policyConfiguration;

    public AbstractAwaitingCycle3DataStateController(
            final T state,
            final HubEventLogger hubEventLogger,
            final StateTransitionAction stateTransitionAction,
            final TransactionsConfigProxy transactionsConfigProxy,
            final ResponseFromHubFactory responseFromHubFactory,
            final PolicyConfiguration policyConfiguration,
            final AssertionRestrictionsFactory assertionRestrictionsFactory,
            final MatchingServiceConfigProxy matchingServiceConfigProxy) {

        this.state = state;
        this.hubEventLogger = hubEventLogger;
        this.stateTransitionAction = stateTransitionAction;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.responseFromHubFactory = responseFromHubFactory;
        this.assertionRestrictionsFactory = assertionRestrictionsFactory;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.policyConfiguration = policyConfiguration;
    }

    public T getState() {
        return state;
    }

    public HubEventLogger getHubEventLogger() {
        return hubEventLogger;
    }

    public MatchingServiceConfigProxy getMatchingServiceConfigProxy() {
        return matchingServiceConfigProxy;
    }

    public PolicyConfiguration getPolicyConfiguration() {
        return policyConfiguration;
    }

    public AssertionRestrictionsFactory getAssertionRestrictionsFactory() {
        return assertionRestrictionsFactory;
    }

    public StateTransitionAction getStateTransitionAction() {
        return stateTransitionAction;
    }

    public TransactionsConfigProxy getTransactionsConfigProxy() {
        return transactionsConfigProxy;
    }

    public ResponseFromHubFactory getResponseFromHubFactory() {
        return responseFromHubFactory;
    }

    public abstract S createAttributeQuery(final Cycle3Dataset cycle3Dataset);

    public abstract void handleCycle3DataSubmitted(final String principalIpAddressAsSeenByHub);

    @Override
    public ResponseProcessingDetails getResponseProcessingDetails() {
        return new ResponseProcessingDetails(
            state.getSessionId(),
            ResponseProcessingStatus.GET_C3_DATA,
            state.getRequestIssuerEntityId());
    }

    @Override
    public ResponseFromHub getErrorResponse() {
        return responseFromHubFactory.createNoAuthnContextResponseFromHub(
            state.getRequestId(),
            state.getRelayState(),
            state.getRequestIssuerEntityId(),
            state.getAssertionConsumerServiceUri());
    }

    public Cycle3AttributeRequestData getCycle3AttributeRequestData() {
        final String entityId = state.getRequestIssuerEntityId();
        final MatchingProcess matchingProcessDto = transactionsConfigProxy.getMatchingProcess(entityId);
        final String attributeName = matchingProcessDto.getAttributeName().get();

        return new Cycle3AttributeRequestData(attributeName, entityId);
    }

    public void handleCancellation() {
        hubEventLogger.logCycle3DataInputCancelled(state.getSessionId(), state.getRequestIssuerEntityId(), state.getSessionExpiryTimestamp(), state.getRequestId());
        Cycle3DataInputCancelledState cycle3DataInputCancelledState = new Cycle3DataInputCancelledState(
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                new SessionId(state.getSessionId().getSessionId()),
                state.getTransactionSupportsEidas());
        stateTransitionAction.transitionTo(cycle3DataInputCancelledState);
    }
}
