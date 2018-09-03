package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.ResponseProcessingDetails;
import uk.gov.ida.hub.policy.domain.ResponseProcessingStatus;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.state.AbstractUserAccountCreationFailedState;

public abstract class AbstractUserAccountCreationFailedStateController<S extends AbstractUserAccountCreationFailedState> implements StateController, ResponseProcessingStateController, ErrorResponsePreparedStateController {

    protected S state;

    private ResponseFromHubFactory responseFromHubFactory;

    public AbstractUserAccountCreationFailedStateController(
            final S state,
            final ResponseFromHubFactory responseFromHubFactory) {

        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
    }

    @Override
    public ResponseProcessingDetails getResponseProcessingDetails() {
        return new ResponseProcessingDetails(
                state.getSessionId(),
                ResponseProcessingStatus.USER_ACCOUNT_CREATION_FAILED,
                state.getRequestIssuerEntityId()
        );
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
}
