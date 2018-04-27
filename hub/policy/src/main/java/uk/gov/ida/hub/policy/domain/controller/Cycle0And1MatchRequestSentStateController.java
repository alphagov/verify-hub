package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.List;

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
    protected State getNextStateForMatch(MatchFromMatchingService responseFromMatchingService) {
        hubEventLogger.logCycle01SuccessfulMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getRequestId(),
                state.getSessionExpiryTimestamp());

        return getSuccessfulMatchState(responseFromMatchingService);
    }

    @Override
    protected State getNextStateForNoMatch() {
        Optional<String> selfAssertedAttributeName = transactionsConfigProxy.getMatchingProcess(state.getRequestIssuerEntityId()).getAttributeName();
        if (selfAssertedAttributeName.isPresent()) {
            hubEventLogger.logWaitingForCycle3AttributesEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());
            return getAwaitingCycle3DataState();
        }

        List<UserAccountCreationAttribute> userAccountCreationAttributes = transactionsConfigProxy.getUserAccountCreationAttributes(state.getRequestIssuerEntityId());
        if(!userAccountCreationAttributes.isEmpty()) {
            AttributeQueryRequestDto attributeQueryRequestDto = createAttributeQuery(userAccountCreationAttributes);
            return handleUserAccountCreationRequestAndGenerateState(attributeQueryRequestDto, state.isRegistering());
        }

        hubEventLogger.logCycle01NoMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getRequestId(),
                state.getSessionExpiryTimestamp());

        return getNoMatchState();
    }

    @Override
    protected SuccessfulMatchState createSuccessfulMatchState(String matchingServiceAssertion, String requestIssuerId) {
        return new SuccessfulMatchState(
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdentityProviderEntityId(),
                matchingServiceAssertion,
                state.getRelayState().orNull(),
                requestIssuerId,
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdpLevelOfAssurance(),
                state.isRegistering(),
                state.getTransactionSupportsEidas()
        );
    }

    @Override
    protected State getNextStateForUserAccountCreated(UserAccountCreatedFromMatchingService responseFromMatchingService) {
        return null;
    }

    @Override
    protected State getNextStateForUserAccountCreationFailed() {
        return null;
    }

    private AwaitingCycle3DataState getAwaitingCycle3DataState() {
        return new AwaitingCycle3DataState(
                state.getRequestId(),
                state.getIdentityProviderEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestIssuerEntityId(),
                state.getEncryptedMatchingDatasetAssertion(),
                state.getAuthnStatementAssertion(),
                state.getRelayState().orNull(),
                state.getAssertionConsumerServiceUri(),
                state.getMatchingServiceAdapterEntityId(),
                state.getSessionId(),
                state.getPersistentId(),
                state.getIdpLevelOfAssurance(),
                state.isRegistering(),
                state.getTransactionSupportsEidas());
    }

    public AttributeQueryRequestDto createAttributeQuery(List<UserAccountCreationAttribute> userAccountCreationAttributes) {
        MatchingServiceConfigEntityDataDto matchingServiceConfig = matchingServiceConfigProxy.getMatchingService(state.getMatchingServiceAdapterEntityId());

        return AttributeQueryRequestDto.createUserAccountRequiredMatchingServiceRequest(
                state.getRequestId(),
                state.getEncryptedMatchingDatasetAssertion(),
                state.getAuthnStatementAssertion(),
                Optional.absent(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getMatchingServiceAdapterEntityId(),
                DateTime.now().plus(policyConfiguration.getMatchingServiceResponseWaitPeriod()),
                state.getIdpLevelOfAssurance(),
                userAccountCreationAttributes,
                state.getPersistentId(),
                assertionRestrictionFactory.getAssertionExpiry(),
                matchingServiceConfig.getUserAccountCreationUri(),
                matchingServiceConfig.isOnboarding());

    }
}
