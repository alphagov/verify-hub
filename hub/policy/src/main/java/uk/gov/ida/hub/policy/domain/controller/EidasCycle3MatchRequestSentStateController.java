package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AbstractAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
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
    protected State getNextStateForMatch(MatchFromMatchingService responseFromMatchingService) {
        hubEventLogger.logCycle3SuccessfulMatchEvent(
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
        if (!userAccountCreationAttributes.isEmpty()) {
            EidasAttributeQueryRequestDto attributeQueryRequestDto = createAttributeQuery(userAccountCreationAttributes);
            return handleUserAccountCreationRequestAndGenerateState(attributeQueryRequestDto, false);
        }

        hubEventLogger.logCycle3NoMatchEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId());


        return getNoMatchState();
    }

    @Override
    protected EidasSuccessfulMatchState createSuccessfulMatchState(String matchingServiceAssertion, String requestIssuerId) {
        return new EidasSuccessfulMatchState(
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdentityProviderEntityId(),
                matchingServiceAssertion,
                state.getRelayState().orNull(),
                requestIssuerId,
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdpLevelOfAssurance(),
                state.getTransactionSupportsEidas()
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

    public EidasAttributeQueryRequestDto createAttributeQuery(List<UserAccountCreationAttribute> userAccountCreationAttributes) {
        MatchingServiceConfigEntityDataDto matchingServiceConfig = matchingServiceConfigProxy.getMatchingService(state.getMatchingServiceAdapterEntityId());

        return new EidasAttributeQueryRequestDto(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionExpiryTimestamp(),
                state.getMatchingServiceAdapterEntityId(),
                matchingServiceConfig.getUserAccountCreationUri(),
                DateTime.now().plus(policyConfiguration.getMatchingServiceResponseWaitPeriod()),
                matchingServiceConfig.isOnboarding(),
                state.getIdpLevelOfAssurance(),
                state.getPersistentId(),
                Optional.absent(),
                Optional.of(userAccountCreationAttributes),
                state.getEncryptedIdentityAssertion()
        );
    }
}
