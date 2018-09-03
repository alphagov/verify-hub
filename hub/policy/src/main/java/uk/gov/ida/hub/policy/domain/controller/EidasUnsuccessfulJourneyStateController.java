package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.StateController;

public interface EidasUnsuccessfulJourneyStateController extends StateController {
    void transitionToSessionStartedState();
}
