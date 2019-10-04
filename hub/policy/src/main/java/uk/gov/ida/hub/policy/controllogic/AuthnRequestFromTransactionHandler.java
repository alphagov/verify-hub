package uk.gov.ida.hub.policy.controllogic;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.AuthnRequestFromHub;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.controller.AuthnFailedErrorStateController;
import uk.gov.ida.hub.policy.domain.controller.AuthnRequestCapableController;
import uk.gov.ida.hub.policy.domain.controller.RestartJourneyStateController;
import uk.gov.ida.hub.policy.domain.controller.NonMatchingJourneySuccessStateController;
import uk.gov.ida.hub.policy.domain.controller.ErrorResponsePreparedStateController;
import uk.gov.ida.hub.policy.domain.controller.IdpSelectingStateController;
import uk.gov.ida.hub.policy.domain.controller.ResponsePreparedStateController;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.RestartJourneyState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingState;
import uk.gov.ida.hub.policy.domain.state.ResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;
import uk.gov.ida.saml.core.domain.AuthnResponseFromCountryContainerDto;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class AuthnRequestFromTransactionHandler {

    private final SessionRepository sessionRepository;
    private final HubEventLogger hubEventLogger;
    private final PolicyConfiguration policyConfiguration;
    private final TransactionsConfigProxy transactionsConfigProxy;
    private final IdGenerator idGenerator;

    @Inject
    public AuthnRequestFromTransactionHandler(
            SessionRepository sessionRepository,
            HubEventLogger hubEventLogger,
            PolicyConfiguration policyConfiguration,
            TransactionsConfigProxy transactionsConfigProxy,
            IdGenerator idGenerator) {

        this.sessionRepository = sessionRepository;
        this.hubEventLogger = hubEventLogger;
        this.policyConfiguration = policyConfiguration;
        this.transactionsConfigProxy = transactionsConfigProxy;
        this.idGenerator = idGenerator;
    }

    public SessionId handleRequestFromTransaction(SamlResponseWithAuthnRequestInformationDto samlResponse, Optional<String> relayState, String ipAddress, URI assertionConsumerServiceUri, boolean transactionSupportsEidas) {
        Duration sessionLength = policyConfiguration.getSessionLength();
        DateTime sessionExpiryTimestamp = DateTime.now().plus(sessionLength);
        SessionId sessionId = SessionId.createNewSessionId();
        SessionStartedState sessionStartedState = new SessionStartedState(
                samlResponse.getId(),
                relayState.orElse(null),
                samlResponse.getIssuer(),
                assertionConsumerServiceUri,
                samlResponse.getForceAuthentication().orElse(null),
                sessionExpiryTimestamp,
                sessionId,
                transactionSupportsEidas);
        final List<LevelOfAssurance> transactionLevelsOfAssurance = transactionsConfigProxy.getLevelsOfAssurance(samlResponse.getIssuer());

        hubEventLogger.logSessionStartedEvent(samlResponse, ipAddress, sessionExpiryTimestamp, sessionId, transactionLevelsOfAssurance.get(0), transactionLevelsOfAssurance.get(transactionLevelsOfAssurance.size() -1));

        return sessionRepository.createSession(sessionStartedState);
    }

    public void tryAnotherIdp(final SessionId sessionId) {
        final AuthnFailedErrorStateController stateController = (AuthnFailedErrorStateController)
                sessionRepository.getStateController(sessionId, AuthnFailedErrorState.class);
        stateController.tryAnotherIdpResponse();
    }

    public void restartJourney(final SessionId sessionId) {
        final RestartJourneyStateController stateController =
                (RestartJourneyStateController) sessionRepository.getStateController(sessionId, RestartJourneyState.class);
        stateController.transitionToSessionStartedState();
    }

    public void selectIdpForGivenSessionId(SessionId sessionId, IdpSelected idpSelected) {
        IdpSelectingStateController stateController = (IdpSelectingStateController)
                sessionRepository.getStateController(sessionId, IdpSelectingState.class);
        stateController.handleIdpSelected(idpSelected.getSelectedIdpEntityId(), idpSelected.getPrincipalIpAddress(), idpSelected.isRegistration(), idpSelected.getRequestedLoa(), idpSelected.getAnalyticsSessionId(), idpSelected.getJourneyType());
    }

    public AuthnRequestFromHub getIdaAuthnRequestFromHub(SessionId sessionId) {
        Class<? extends State> currentState = sessionRepository.isSessionInState(sessionId, EidasCountrySelectedState.class) ? EidasCountrySelectedState.class : IdpSelectedState.class;
        AuthnRequestCapableController stateController = (AuthnRequestCapableController)
                sessionRepository.getStateController(sessionId, currentState);
        return stateController.getRequestFromHub();
    }

    public AuthnRequestSignInProcess getSignInProcessDto(SessionId sessionIdParameter) {
        IdpSelectingStateController stateController = (IdpSelectingStateController)
                sessionRepository.getStateController(sessionIdParameter, IdpSelectingState.class);
        return stateController.getSignInProcessDetails();
    }

    public String getRequestIssuerId(SessionId sessionId) {
        IdpSelectingStateController stateController = (IdpSelectingStateController)
                sessionRepository.getStateController(sessionId, IdpSelectingState.class);
        return stateController.getRequestIssuerId();
    }

    public ResponseFromHub getResponseFromHub(SessionId sessionId) {
        ResponsePreparedStateController stateController = (ResponsePreparedStateController)
                sessionRepository.getStateController(sessionId, ResponsePreparedState.class);
        return stateController.getPreparedResponse();
    }

    public ResponseFromHub getErrorResponseFromHub(SessionId sessionId) {
        ErrorResponsePreparedStateController stateController = (ErrorResponsePreparedStateController)
                sessionRepository.getStateController(sessionId, ErrorResponsePreparedState.class);
        return stateController.getErrorResponse();
    }

    public AuthnResponseFromCountryContainerDto getAuthnResponseFromCountryContainerDto(SessionId sessionId) {
        NonMatchingJourneySuccessStateController stateController = (NonMatchingJourneySuccessStateController)
                sessionRepository.getStateController(sessionId, ResponsePreparedState.class);
        NonMatchingJourneySuccessState state = stateController.getState();
        return new AuthnResponseFromCountryContainerDto(
                state.getCountrySignedResponseContainer().get(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                idGenerator.getId()
        );
    }

    public boolean isResponseFromCountryWithUnsignedAssertions(SessionId sessionId) {
        try {
            NonMatchingJourneySuccessStateController stateController = (NonMatchingJourneySuccessStateController)
                    sessionRepository.getStateController(sessionId, ResponsePreparedState.class);
            NonMatchingJourneySuccessState state = stateController.getState();
            return state.getCountrySignedResponseContainer().isPresent();
        } catch (ClassCastException e) {
            return false;
        }
    }
}
