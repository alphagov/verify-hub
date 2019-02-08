package uk.gov.ida.hub.policy.domain.controller;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
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
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.hub.policy.services.AttributeQueryService;
import uk.gov.ida.hub.policy.validators.LevelOfAssuranceValidator;

import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.wrongInResponseTo;
import static uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException.wrongResponseIssuer;

public abstract class AbstractMatchRequestSentStateController<T extends AbstractMatchRequestSentState, U extends AbstractSuccessfulMatchState> implements ResponseProcessingStateController, WaitingForMatchingServiceResponseStateController, ErrorResponsePreparedStateController {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMatchRequestSentStateController.class);

    private final ResponseFromHubFactory responseFromHubFactory;

    protected final T state;
    protected final HubEventLogger hubEventLogger;
    protected final PolicyConfiguration policyConfiguration;
    protected final LevelOfAssuranceValidator levelOfAssuranceValidator;
    protected final AttributeQueryService attributeQueryService;
    protected final StateTransitionAction stateTransitionAction;
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
        this.levelOfAssuranceValidator = validator;
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
                state.getRelayState().orNull(),
                state.getSessionId(),
                state.getTransactionSupportsEidas());
        stateTransitionAction.transitionTo(matchingServiceRequestErrorState);
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
                    state.getRelayState().orNull(),
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

    @Override
    public final void handleMatchResponseFromMatchingService(MatchFromMatchingService responseFromMatchingService) {
        validateResponse(responseFromMatchingService);
        levelOfAssuranceValidator.validate(responseFromMatchingService.getLevelOfAssurance(), state.getIdpLevelOfAssurance());
        transitionToNextStateForMatchResponse(responseFromMatchingService.getMatchingServiceAssertion());
    }

    @Override
    public final void handleNoMatchResponseFromMatchingService(NoMatchFromMatchingService responseFromMatchingService) {
        validateResponse(responseFromMatchingService);
        transitionToNextStateForNoMatchResponse();
    }

    @Override
    public final void handleUserAccountCreatedResponseFromMatchingService(UserAccountCreatedFromMatchingService userAccountCreatedResponseFromMatchingService) {
        validateResponse(userAccountCreatedResponseFromMatchingService);
        levelOfAssuranceValidator.validate(userAccountCreatedResponseFromMatchingService.getLevelOfAssurance(), state.getIdpLevelOfAssurance());
        hubEventLogger.logUserAccountCreatedEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());
        transitionToNextStateForUserAccountCreatedResponse(userAccountCreatedResponseFromMatchingService);
    }

    @Override
    public final void handleUserAccountCreationFailedResponseFromMatchingService() {
        hubEventLogger.logUserAccountCreationFailedEvent(state.getSessionId(), state.getRequestIssuerEntityId(), state.getRequestId(), state.getSessionExpiryTimestamp());
        transitionToNextStateForUserAccountCreationFailedResponse();
    }

    protected abstract U createSuccessfulMatchState(String matchingServiceAssertion);

    protected abstract State createUserAccountCreationRequestSentState();

    protected void transitionToNextStateForNoMatchResponse() {
        // Do nothing by default. Subclasses can override this method and provide logic for the transition.
    }

    protected void transitionToNextStateForMatchResponse(String matchingServiceAssertion) {
        // Do nothing by default. Subclasses can override this method and provide logic for the transition.
    }

    protected void transitionToNextStateForUserAccountCreatedResponse(UserAccountCreatedFromMatchingService responseFromMatchingService) {
        // Do nothing by default. Subclasses can override this method and provide logic for the transition.
    }

    protected void transitionToNextStateForUserAccountCreationFailedResponse() {
        // Do nothing by default. Subclasses can override this method and provide logic for the transition.
    }

    protected void validateResponse(ResponseFromMatchingService responseFromMatchingService) {
        if (!responseFromMatchingService.getIssuer().equals(state.getMatchingServiceAdapterEntityId())) {
            throw wrongResponseIssuer(state.getRequestId(), responseFromMatchingService.getIssuer(), state.getMatchingServiceAdapterEntityId());
        }

        if (!this.state.getRequestId().equals(responseFromMatchingService.getInResponseTo())) {
            throw wrongInResponseTo(this.state.getRequestId(), responseFromMatchingService.getInResponseTo());
        }
    }

    protected void transitionToUserAccountCreationRequestSentState(AbstractAttributeQueryRequestDto attributeQueryRequestDto) {
        hubEventLogger.logMatchingServiceUserAccountCreationRequestSentEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId());

        attributeQueryService.sendAttributeQueryRequest(state.getSessionId(), attributeQueryRequestDto);

        stateTransitionAction.transitionTo(createUserAccountCreationRequestSentState());
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

    protected NoMatchState createNoMatchState() {
        return new NoMatchState(
                state.getRequestId(),
                state.getIdentityProviderEntityId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState().orNull(),
                state.getSessionId(),
                state.getTransactionSupportsEidas());
    }
}
