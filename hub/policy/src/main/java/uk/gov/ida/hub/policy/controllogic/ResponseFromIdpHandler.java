package uk.gov.ida.hub.policy.controllogic;

import uk.gov.ida.hub.policy.domain.FailureResponseDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.AuthnFailedErrorStateController;
import uk.gov.ida.hub.policy.domain.controller.ResponseProcessingStateController;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.ResponseProcessingState;

import javax.inject.Inject;

public class ResponseFromIdpHandler {

    private final SessionRepository sessionRepository;

    @Inject
    public ResponseFromIdpHandler(SessionRepository sessionRepository) {

        this.sessionRepository = sessionRepository;
    }

    public ResponseProcessingDetails getResponseProcessingDetails(SessionId sessionId) {
        ResponseProcessingStateController stateController = (ResponseProcessingStateController) sessionRepository.getStateController(sessionId, ResponseProcessingState.class);
        return stateController.getResponseProcessingDetails();
    }

    public FailureResponseDetails getErrorResponseFromIdp(SessionId sessionId) {
        AuthnFailedErrorStateController stateController = (AuthnFailedErrorStateController) sessionRepository.getStateController(sessionId, AuthnFailedErrorStateTransitional.class);
        return stateController.handleFailureResponse();
    }
}
