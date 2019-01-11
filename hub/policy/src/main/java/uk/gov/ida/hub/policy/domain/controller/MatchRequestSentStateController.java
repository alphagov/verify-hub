package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.List;

public abstract class MatchRequestSentStateController<T extends MatchRequestSentState> extends AbstractMatchRequestSentStateController<T, SuccessfulMatchState> {

    public MatchRequestSentStateController(
            final T state,
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
    protected SuccessfulMatchState createSuccessfulMatchState(String matchingServiceAssertion) {
        return new SuccessfulMatchState(
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdentityProviderEntityId(),
                matchingServiceAssertion,
                state.getRelayState().orNull(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdpLevelOfAssurance(),
                state.isRegistering(),
                state.getTransactionSupportsEidas()
        );
    }

    @Override
    protected State createUserAccountCreationRequestSentState() {
        return new UserAccountCreationRequestSentState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getTransactionSupportsEidas(),
                state.getIdentityProviderEntityId(),
                state.getRelayState().orNull(),
                state.getIdpLevelOfAssurance(),
                state.isRegistering(),
                state.getMatchingServiceAdapterEntityId()
        );
    }

    protected AttributeQueryRequestDto createAttributeQuery(
            List<UserAccountCreationAttribute> userAccountCreationAttributes,
            String encryptedMatchingDatasetAssertion,
            String authnStatementAssertion,
            PersistentId persistentId,
            DateTime assertionExpiry) {

        MatchingServiceConfigEntityDataDto matchingServiceConfig = matchingServiceConfigProxy.getMatchingService(state.getMatchingServiceAdapterEntityId());

        return AttributeQueryRequestDto.createUserAccountRequiredMatchingServiceRequest(
                state.getRequestId(),
                encryptedMatchingDatasetAssertion,
                authnStatementAssertion,
                com.google.common.base.Optional.absent(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getMatchingServiceAdapterEntityId(),
                DateTime.now().plus(policyConfiguration.getMatchingServiceResponseWaitPeriod()),
                state.getIdpLevelOfAssurance(),
                userAccountCreationAttributes,
                persistentId,
                assertionExpiry,
                matchingServiceConfig.getUserAccountCreationUri(),
                matchingServiceConfig.isOnboarding());

    }
}
