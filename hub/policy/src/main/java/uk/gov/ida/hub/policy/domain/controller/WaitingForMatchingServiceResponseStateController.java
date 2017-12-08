package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.NoMatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;

public interface WaitingForMatchingServiceResponseStateController extends StateController {
    void handleMatchResponseFromMatchingService(MatchFromMatchingService responseFromMatchingService);

    void handleNoMatchResponseFromMatchingService(NoMatchFromMatchingService noMatchResponseFromMatchingService);

    void handleRequestFailure();

    void handleUserAccountCreatedResponseFromMatchingService(UserAccountCreatedFromMatchingService userAccountCreatedResponseFromMatchingService);

    void handleUserAccountCreationFailedResponseFromMatchingService();
}
