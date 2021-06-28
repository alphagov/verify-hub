package uk.gov.ida.hub.policy.services;

import com.google.inject.Inject;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseDto;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.NoMatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.controller.WaitingForMatchingServiceResponseStateController;
import uk.gov.ida.hub.policy.domain.exception.SessionNotFoundException;
import uk.gov.ida.hub.policy.domain.state.WaitingForMatchingServiceResponseState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

public class MatchingServiceResponseService {

    private static final Logger LOG = Logger.getLogger(MatchingServiceResponseService.class.getName());

    private final SamlEngineProxy samlEngineProxy;
    private final SessionRepository sessionRepository;
    private final HubEventLogger eventLogger;

    @Inject
    public MatchingServiceResponseService(SamlEngineProxy samlEngineProxy,
                                          SessionRepository sessionRepository,
                                          HubEventLogger eventLogger) {
        this.samlEngineProxy = samlEngineProxy;
        this.sessionRepository = sessionRepository;
        this.eventLogger = eventLogger;
    }

    public void handleFailure(SessionId sessionId) {
        getSessionIfItExists(sessionId);
        logRequesterErrorAndUpdateState(sessionId, "received failure notification from saml-soap-proxy");
    }

    public void handleSuccessResponse(SessionId sessionId, SamlResponseDto samlResponse) {
        getSessionIfItExists(sessionId);

        SamlResponseContainerDto samlResponseContainer = new SamlResponseContainerDto(
            samlResponse.getSamlResponse(),
            sessionRepository.getRequestIssuerEntityId(sessionId)
        );

        try {
            InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto = samlEngineProxy.translateMatchingServiceResponse(samlResponseContainer);
            updateSessionState(sessionId, inboundResponseFromMatchingServiceDto);
        } catch(ApplicationException e) {
            // this is not ideal but if the call to saml-proxy fails we want to log the failure
            // in the state and then process the exception
            logExceptionAndUpdateState("Error translating matching service response", sessionId, e);
        }
    }

    private void updateSessionState(final SessionId sessionId, final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto) {
        switch (inboundResponseFromMatchingServiceDto.getStatus()) {

            case RequesterError:
                logRequesterErrorAndUpdateState(sessionId, "Requester error in response from matching service");
                break;

            case UserAccountCreated:
                handleUserAccountCreatedResponseFromMatchingService(sessionId, inboundResponseFromMatchingServiceDto);
                break;

            case UserAccountCreationFailed:
                handleUserAccountCreationFailedResponseFromMatchingService(sessionId);
                break;

            case NoMatchingServiceMatchFromMatchingService:
                handleNoMatchResponseFromMatchingService(sessionId, inboundResponseFromMatchingServiceDto);
                break;

            case MatchingServiceMatch:
                handleMatchResponseFromMatchingService(sessionId, inboundResponseFromMatchingServiceDto);
                break;

            default:
                LOG.log(Level.SEVERE, format("Unknown status received from matching service: {0}", inboundResponseFromMatchingServiceDto.getStatus()));
        }
    }

    private void logExceptionAndUpdateState(String message, SessionId sessionId, Exception e) {
        LOG.log(Level.INFO, message, e);
        logToEventSinkAndUpdateState(message, sessionId);
    }

    private void logRequesterErrorAndUpdateState(SessionId sessionId, String msg){
        final String message = format("{0} for session {1}", msg, sessionId);
        LOG.info(message);
        logToEventSinkAndUpdateState(message, sessionId);
    }

    private void logToEventSinkAndUpdateState(String message, SessionId sessionId) {
        eventLogger.logErrorEvent(message, sessionId);
        handleHubMatchingServiceRequestFailure(sessionId);
    }

    private void handleMatchResponseFromMatchingService(
            SessionId sessionId,
            InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto) {

        MatchFromMatchingService matchFromMatchingService =
                new MatchFromMatchingService(
                        inboundResponseFromMatchingServiceDto.getIssuer(),
                        inboundResponseFromMatchingServiceDto.getInResponseTo(),
                        inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().get(),
                        inboundResponseFromMatchingServiceDto.getLevelOfAssurance());

        WaitingForMatchingServiceResponseStateController stateController = (WaitingForMatchingServiceResponseStateController) sessionRepository.getStateController(sessionId, WaitingForMatchingServiceResponseState.class);
        stateController.handleMatchResponseFromMatchingService(matchFromMatchingService);
    }

    private void handleNoMatchResponseFromMatchingService(
            SessionId sessionId,
            InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto) {

        NoMatchFromMatchingService noMatchFromMatchingService =
                new NoMatchFromMatchingService(inboundResponseFromMatchingServiceDto.getIssuer(), inboundResponseFromMatchingServiceDto.getInResponseTo());

        WaitingForMatchingServiceResponseStateController stateController = (WaitingForMatchingServiceResponseStateController) sessionRepository.getStateController(sessionId, WaitingForMatchingServiceResponseState.class);
        stateController.handleNoMatchResponseFromMatchingService(noMatchFromMatchingService);
    }

    private void handleHubMatchingServiceRequestFailure(SessionId sessionId) {
        WaitingForMatchingServiceResponseStateController stateController = (WaitingForMatchingServiceResponseStateController) sessionRepository.getStateController(sessionId, WaitingForMatchingServiceResponseState.class);
        stateController.handleRequestFailure();
    }

    private void handleUserAccountCreatedResponseFromMatchingService(
            SessionId sessionId,
            InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto) {

        UserAccountCreatedFromMatchingService userAccountCreatedFromMatchingService =
                new UserAccountCreatedFromMatchingService(
                        inboundResponseFromMatchingServiceDto.getIssuer(),
                        inboundResponseFromMatchingServiceDto.getInResponseTo(),
                        inboundResponseFromMatchingServiceDto.getEncryptedMatchingServiceAssertion().get(),
                        inboundResponseFromMatchingServiceDto.getLevelOfAssurance());

        WaitingForMatchingServiceResponseStateController stateController = (WaitingForMatchingServiceResponseStateController) sessionRepository.getStateController(sessionId, WaitingForMatchingServiceResponseState.class);
        stateController.handleUserAccountCreatedResponseFromMatchingService(userAccountCreatedFromMatchingService);
    }

    private void handleUserAccountCreationFailedResponseFromMatchingService(
        SessionId sessionId) {

        WaitingForMatchingServiceResponseStateController stateController =
            (WaitingForMatchingServiceResponseStateController) sessionRepository.getStateController(
                sessionId, WaitingForMatchingServiceResponseState.class
            );

        stateController.handleUserAccountCreationFailedResponseFromMatchingService();
    }

    private SessionId getSessionIfItExists(SessionId sessionId) {
        if (sessionRepository.sessionExists(sessionId)) {
            return sessionId;
        }

        throw new SessionNotFoundException(sessionId);
    }
}
