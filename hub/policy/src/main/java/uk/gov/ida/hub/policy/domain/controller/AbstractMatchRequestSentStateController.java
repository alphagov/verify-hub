package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AbstractAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.NoMatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseFromMatchingService;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.state.AbstractMatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.wrongInResponseTo;
import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.wrongResponseIssuer;

public abstract class AbstractMatchRequestSentStateController<T extends AbstractMatchRequestSentState, U extends AbstractSuccessfulMatchState> implements ResponseProcessingStateController, WaitingForMatchingServiceResponseStateController, ErrorResponsePreparedStateController {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMatchRequestSentStateController.class);

    private final StateTransitionAction stateTransitionAction;
    private final LevelOfAssuranceValidator validator;
    private final ResponseFromHubFactory responseFromHubFactory;

    protected final T state;
    protected final HubEventLogger hubEventLogger;
    protected PolicyConfiguration policyConfiguration;
    protected AttributeQueryService attributeQueryService;
    protected final TransactionsConfigProxy transactionsConfigProxy;
    protected final MatchingServiceConfigProxy matchingServiceConfigProxy;

    public AbstractMatchRequestSentStateController(
            final T state,
            final StateTransitionAction stateTransitionAction,
            final HubEventLogger hubEventLogger,
            final PolicyConfiguration policyConfiguration,
            final LevelOfAssuranceValidator validator,
            final ResponseFromHubFactory responseFromHubFactory,
            final AttributeQueryService attributeQueryService,
            final TransactionsConfigProxy transactionsConfigProxy,
            final MatchingServiceConfigProxy matchingServiceConfigProxy) {

        this.state = state;
        this.stateTransitionAction = stateTransitionAction;
        this.hubEventLogger = hubEventLogger;
        this.validator = validator;
        this.responseFromHubFactory = responseFromHubFactory;
        this.policyConfiguration = policyConfiguration;
        this.attributeQueryService = attributeQueryService;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
    }

    @Override
    public void handleRequestFailure() {
        MatchingServiceRequestErrorState matchingServiceRequestErrorState = new MatchingServiceRequestErrorState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getIdentityProviderEntityId(),
                state.getRelayState(),
                state.getSessionId(),
                state.getTransactionSupportsEidas());
        stateTransitionAction.transitionTo(matchingServiceRequestErrorState);
    }

    protected U getSuccessfulMatchState(MatchFromMatchingService responseFromMatchingService) {
        String matchingServiceAssertion = responseFromMatchingService.getMatchingServiceAssertion();
        validator.validate(responseFromMatchingService.getLevelOfAssurance(), state.getIdpLevelOfAssurance());
        String requestIssuerId = state.getRequestIssuerEntityId();
        return createSuccessfulMatchState(matchingServiceAssertion, requestIssuerId);
    }

    @Override
    public final void handleMatchResponseFromMatchingService(MatchFromMatchingService responseFromMatchingService) {
        validateResponse(responseFromMatchingService);

        State nextState = getNextStateForMatch(responseFromMatchingService);

        stateTransitionAction.transitionTo(nextState);
    }

    @Override
    public final void handleNoMatchResponseFromMatchingService(NoMatchFromMatchingService responseFromMatchingService) {
        validateResponse(responseFromMatchingService);

        State nextState = getNextStateForNoMatch();

        stateTransitionAction.transitionTo(nextState);
    }


    @Override
    public final void handleUserAccountCreatedResponseFromMatchingService(UserAccountCreatedFromMatchingService userAccountCreatedResponseFromMatchingService) {
        validateResponse(userAccountCreatedResponseFromMatchingService);

        State nextState = getNextStateForUserAccountCreated(userAccountCreatedResponseFromMatchingService);

        stateTransitionAction.transitionTo(nextState);
    }

    @Override
    public final void handleUserAccountCreationFailedResponseFromMatchingService() {
        stateTransitionAction.transitionTo(getNextStateForUserAccountCreationFailed());
    }

    protected abstract State getNextStateForUserAccountCreated(UserAccountCreatedFromMatchingService responseFromMatchingService);

    protected abstract State getNextStateForUserAccountCreationFailed();

    protected abstract State getNextStateForMatch(MatchFromMatchingService responseFromMatchingService);

    protected abstract State getNextStateForNoMatch();

    protected abstract U createSuccessfulMatchState(String matchingServiceAssertion, String requestIssuerId);

    protected void validateResponse(ResponseFromMatchingService responseFromMatchingService) {
        if (!responseFromMatchingService.getIssuer().equals(state.getMatchingServiceAdapterEntityId())) {
            throw wrongResponseIssuer(state.getRequestId(), responseFromMatchingService.getIssuer(), state.getMatchingServiceAdapterEntityId());
        }

        if (!this.state.getRequestId().equals(responseFromMatchingService.getInResponseTo())) {
            throw wrongInResponseTo(this.state.getRequestId(), responseFromMatchingService.getInResponseTo());
        }
    }

    @Override
    public ResponseProcessingDetails getResponseProcessingDetails() {
        if (matchingServiceRequestHasTimedOut()) {
            LOG.error("Matching service request timed out for session {}", state.getSessionId());
            MatchingServiceRequestErrorState matchingServiceRequestErrorState = new MatchingServiceRequestErrorState(
                    state.getRequestId(),
                    state.getRequestIssuerEntityId(),
                    state.getSessionExpiryTimestamp(),
                    state.getAssertionConsumerServiceUri(),
                    state.getIdentityProviderEntityId(),
                    state.getRelayState(),
                    state.getSessionId(),
                    state.getTransactionSupportsEidas());

            stateTransitionAction.transitionTo(matchingServiceRequestErrorState);
            return createResponseProcessingDetailsForStatus(ResponseProcessingStatus.SHOW_MATCHING_ERROR_PAGE);
        }

        return createResponseProcessingDetailsForStatus(ResponseProcessingStatus.WAIT);
    }

    @Override
    public ResponseFromHub getErrorResponse() {
        return responseFromHubFactory.createNoAuthnContextResponseFromHub(
                state.getRequestId(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri()
        );
    }

    private ResponseProcessingDetails createResponseProcessingDetailsForStatus(ResponseProcessingStatus responseProcessingStatus) {
        return new ResponseProcessingDetails(
                state.getSessionId(),
                responseProcessingStatus,
                state.getRequestIssuerEntityId()
        );
    }

    private boolean matchingServiceRequestHasTimedOut() {
        return DateTime.now().isAfter(state.getRequestSentTime().plus(policyConfiguration.getMatchingServiceResponseWaitPeriod()));
    }

    NoMatchState getNoMatchState() {
        return new NoMatchState(
                state.getRequestId(),
                state.getIdentityProviderEntityId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getSessionId(),
                state.getTransactionSupportsEidas());
    }

    State handleUserAccountCreationRequestAndGenerateState(
            AbstractAttributeQueryRequestDto attributeQueryRequestDto,
            boolean isRegistering) {
        hubEventLogger.logMatchingServiceUserAccountCreationRequestSentEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId());

        attributeQueryService.sendAttributeQueryRequest(state.getSessionId(), attributeQueryRequestDto);

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
                isRegistering,
                state.getMatchingServiceAdapterEntityId()
        );
    }
}
