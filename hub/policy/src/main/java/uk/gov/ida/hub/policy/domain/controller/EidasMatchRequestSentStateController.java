package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.hub.policy.domain.state.EidasMatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import java.util.List;

public abstract class EidasMatchRequestSentStateController<T extends EidasMatchRequestSentState> extends AbstractMatchRequestSentStateController<T, EidasSuccessfulMatchState> {

    public EidasMatchRequestSentStateController(
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
    protected EidasSuccessfulMatchState createSuccessfulMatchState(String matchingServiceAssertion) {
        return new EidasSuccessfulMatchState(
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdentityProviderEntityId(),
                matchingServiceAssertion,
                state.getRelayState().orNull(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdpLevelOfAssurance(),
                state.getTransactionSupportsEidas()
        );
    }

    @Override
    protected State createUserAccountCreationRequestSentState() {
        return new EidasUserAccountCreationRequestSentState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdentityProviderEntityId(),
                state.getRelayState().orNull(),
                state.getIdpLevelOfAssurance(),
                state.getMatchingServiceAdapterEntityId(),
                state.getForceAuthentication().orNull());
    }

    public EidasAttributeQueryRequestDto createAttributeQuery(
            List<UserAccountCreationAttribute> userAccountCreationAttributes, String encryptedIdentityAssertion, PersistentId persistentId) {

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
                persistentId,
                Optional.absent(),
                Optional.of(userAccountCreationAttributes),
                encryptedIdentityAssertion);
    }
}
