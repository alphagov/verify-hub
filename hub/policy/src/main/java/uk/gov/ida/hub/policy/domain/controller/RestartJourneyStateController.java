package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.StateController;

public interface RestartJourneyStateController extends StateController {
    void transitionToSessionStartedState();
}
