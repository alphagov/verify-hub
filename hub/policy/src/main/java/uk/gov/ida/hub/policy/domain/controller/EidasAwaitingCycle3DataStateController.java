package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class EidasAwaitingCycle3DataStateController extends AbstractAwaitingCycle3DataStateController<EidasAttributeQueryRequestDto, EidasAwaitingCycle3DataState> implements StateController, ResponseProcessingStateController, ErrorResponsePreparedStateController {

    public EidasAwaitingCycle3DataStateController(
        final EidasAwaitingCycle3DataState state,
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
    public EidasAttributeQueryRequestDto createAttributeQuery(final Cycle3Dataset cycle3Dataset) {
        MatchingServiceConfigEntityDataDto matchingServiceConfigData = getMatchingServiceConfigProxy().getMatchingService(getState().getMatchingServiceEntityId());
        URI matchingServiceAdapterUri = matchingServiceConfigData.getUri();

        return new EidasAttributeQueryRequestDto(
            getState().getRequestId(),
            getState().getRequestIssuerEntityId(),
            getState().getAssertionConsumerServiceUri(),
            getAssertionRestrictionsFactory().getAssertionExpiry(),
            getState().getMatchingServiceEntityId(),
            matchingServiceAdapterUri,
            DateTime.now().plus(getPolicyConfiguration().getMatchingServiceResponseWaitPeriod()),
            matchingServiceConfigData.isOnboarding(),
            getState().getLevelOfAssurance(),
            getState().getPersistentId(),
            Optional.ofNullable(cycle3Dataset),
            Optional.empty(),
            getState().getEncryptedIdentityAssertion()
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

        EidasCycle3MatchRequestSentState eidasCycle3MatchRequestSentState = new EidasCycle3MatchRequestSentState(
            getState().getRequestId(),
            getState().getRequestIssuerEntityId(),
            getState().getSessionExpiryTimestamp(),
            getState().getAssertionConsumerServiceUri(),
            getState().getSessionId(),
            getState().getTransactionSupportsEidas(),
            getState().getIdentityProviderEntityId(),
            getState().getRelayState().orElse(null),
            getState().getLevelOfAssurance(),
            getState().getMatchingServiceEntityId(),
            getState().getEncryptedIdentityAssertion(),
            getState().getPersistentId(),
            getState().getForceAuthentication().orElse(null)
        );

        getStateTransitionAction().transitionTo(eidasCycle3MatchRequestSentState);
    }
}
