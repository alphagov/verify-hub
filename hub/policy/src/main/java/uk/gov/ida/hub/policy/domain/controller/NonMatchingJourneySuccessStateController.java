package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;

import java.util.ArrayList;

public class NonMatchingJourneySuccessStateController implements StateController, ResponsePreparedStateController {

    private final NonMatchingJourneySuccessState state;
    private final ResponseFromHubFactory responseFromHubFactory;

    public NonMatchingJourneySuccessStateController(
        final NonMatchingJourneySuccessState state,
        final ResponseFromHubFactory responseFromHubFactory) {

        this.state = state;
        this.responseFromHubFactory = responseFromHubFactory;
    }

    @Override
    public ResponseFromHub getPreparedResponse() {
        return responseFromHubFactory.createNonMatchingSuccessResponseFromHub(
                state.getRequestId(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                new ArrayList<>(state.getEncryptedAssertions()),
                state.getAssertionConsumerServiceUri()
        );
    }

    public NonMatchingJourneySuccessState getState() {
      return state;
    };
}
