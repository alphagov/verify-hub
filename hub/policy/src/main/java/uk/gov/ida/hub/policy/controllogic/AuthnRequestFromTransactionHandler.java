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
import uk.gov.ida.hub.policy.domain.controller.ErrorResponsePreparedStateController;
import uk.gov.ida.hub.policy.domain.controller.IdpSelectingStateController;
import uk.gov.ida.hub.policy.domain.controller.ResponsePreparedStateController;
import uk.gov.ida.hub.policy.domain.controller.RestartJourneyStateController;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.ErrorResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingState;
import uk.gov.ida.hub.policy.domain.state.ResponsePreparedState;
import uk.gov.ida.hub.policy.domain.state.RestartJourneyState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
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

    public SessionId handleRequestFromTransaction(SamlResponseWithAuthnRequestInformationDto samlResponse, Optional<String> relayState, String ipAddress, URI assertionConsumerServiceUri) {
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
                sessionId);
        final List<LevelOfAssurance> transactionLevelsOfAssurance = transactionsConfigProxy.getLevelsOfAssurance(samlResponse.getIssuer());

        hubEventLogger.logSessionStartedEvent(
            samlResponse,
            ipAddress,
            sessionExpiryTimestamp,
            sessionId,
            Collections.min(transactionLevelsOfAssurance), // min LOA
            Collections.max(transactionLevelsOfAssurance), // max LOA
            transactionLevelsOfAssurance.get(0)); // preferred LOA

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
        stateController.handleIdpSelected(idpSelected.getSelectedIdpEntityId(), idpSelected.getPrincipalIpAddress(), idpSelected.isRegistration(), idpSelected.getRequestedLoa(), idpSelected.getAnalyticsSessionId(), idpSelected.getJourneyType(), idpSelected.getAbTestVariant());
    }

    public AuthnRequestFromHub getIdaAuthnRequestFromHub(SessionId sessionId) {
        Class<? extends State> currentState = IdpSelectedState.class;
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

}
