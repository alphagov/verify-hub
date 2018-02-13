package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;

public class AwaitingCycle3DataStateController extends AbstractAwaitingCycle3DataStateController<AttributeQueryRequestDto, AwaitingCycle3DataState> implements StateController, ResponseProcessingStateController, ErrorResponsePreparedStateController {

    public AwaitingCycle3DataStateController(
        final AwaitingCycle3DataState state,
        final HubEventLogger hubEventLogger,
        final StateTransitionAction stateTransitionAction,
        final TransactionsConfigProxy transactionsConfigProxy,
        final ResponseFromHubFactory responseFromHubFactory,
        final PolicyConfiguration policyConfiguration,
        final AssertionRestrictionsFactory assertionRestrictionsFactory,
        final MatchingServiceConfigProxy matchingServiceConfigProxy) {
        super(
            state,
            hubEventLogger,
            stateTransitionAction,
            transactionsConfigProxy,
            responseFromHubFactory,
            policyConfiguration,
            assertionRestrictionsFactory,
            matchingServiceConfigProxy);
    }

    @Override
    public AttributeQueryRequestDto createAttributeQuery(final Cycle3Dataset cycle3Dataset) {
        MatchingServiceConfigEntityDataDto matchingServiceConfigData = getMatchingServiceConfigProxy().getMatchingService(getState().getMatchingServiceEntityId());
        URI matchingServiceAdapterUri = matchingServiceConfigData.getUri();

        return AttributeQueryRequestDto.createCycle3MatchingServiceRequest(
            getState().getRequestId(),
            getState().getEncryptedMatchingDatasetAssertion(),
            getState().getAuthnStatementAssertion(),
            cycle3Dataset,
            getState().getRequestIssuerEntityId(),
            getState().getAssertionConsumerServiceUri(),
            getState().getMatchingServiceEntityId(),
            DateTime.now().plus(getPolicyConfiguration().getMatchingServiceResponseWaitPeriod()),
            getState().getLevelOfAssurance(),
            Optional.absent(),
            getState().getPersistentId(),
            getAssertionRestrictionsFactory().getAssertionExpiry(),
            matchingServiceAdapterUri,
            matchingServiceConfigData.isOnboarding()
        );

    }

    @Override
    public void handleCycle3DataSubmitted(final String principalIpAddressAsSeenByHub) {
        getHubEventLogger().logCycle3DataObtained(
            getState().getSessionId(),
            getState().getRequestIssuerEntityId(),
            getState().getSessionExpiryTimestamp(),
            getState().getRequestId(),
            principalIpAddressAsSeenByHub
        );

        Cycle3MatchRequestSentState cycle3MatchRequestSentState = new Cycle3MatchRequestSentState(
            getState().getRequestId(),
            getState().getRequestIssuerEntityId(),
            getState().getSessionExpiryTimestamp(),
            getState().getAssertionConsumerServiceUri(),
            getState().getSessionId(),
            getState().getTransactionSupportsEidas(),
            getState().getIdentityProviderEntityId(),
            getState().getRelayState().orNull(),
            getState().getLevelOfAssurance(),
            getState().isRegistering(),
            getState().getMatchingServiceEntityId(),
            getState().getEncryptedMatchingDatasetAssertion(),
            getState().getAuthnStatementAssertion(),
            getState().getPersistentId()
        );

        getStateTransitionAction().transitionTo(cycle3MatchRequestSentState);
    }
}
