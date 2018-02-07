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
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.List;

public class Cycle3MatchRequestSentStateController extends MatchRequestSentStateController<Cycle3MatchRequestSentStateTransitional> {

    private final TransactionsConfigProxy transactionsConfigProxy;
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final AssertionRestrictionsFactory assertionRestrictionFactory;

    public Cycle3MatchRequestSentStateController(
            final Cycle3MatchRequestSentStateTransitional state,
            final EventSinkHubEventLogger eventSinkHubEventLogger,
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
                eventSinkHubEventLogger,
                policyConfiguration,
                validator,
                responseFromHubFactory,
                attributeQueryService);

        this.transactionsConfigProxy = transactionsConfigProxy;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.assertionRestrictionFactory = assertionRestrictionFactory;
    }

    @Override
    protected State getNextStateForMatch(MatchFromMatchingService responseFromMatchingService) {
        eventSinkHubEventLogger.logCycle3SuccessfulMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId()
        );
        return getSuccessfulMatchState(responseFromMatchingService);
    }

    @Override
    protected State getNextStateForNoMatch() {
        List<UserAccountCreationAttribute> userAccountCreationAttributes = transactionsConfigProxy.getUserAccountCreationAttributes(state.getRequestIssuerEntityId());
        if(!userAccountCreationAttributes.isEmpty()) {
            AttributeQueryRequestDto attributeQueryRequestDto = createAttributeQuery(userAccountCreationAttributes);
            return handleUserAccountCreationRequestAndGenerateState(attributeQueryRequestDto);
        }

        eventSinkHubEventLogger.logCycle3NoMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId());


        return getNoMatchState();
    }

    private AttributeQueryRequestDto createAttributeQuery(List<UserAccountCreationAttribute> userAccountCreationAttributes) {
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
                matchingServiceConfig.isOnboarding()
        );
    }

    @Override
    protected State getNextStateForUserAccountCreated(final UserAccountCreatedFromMatchingService responseFromMatchingService) {
        return null;
    }

    @Override
    protected State getNextStateForUserAccountCreationFailed() {
        return null;
    }
}
