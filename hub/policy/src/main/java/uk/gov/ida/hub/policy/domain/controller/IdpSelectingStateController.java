package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;

public interface IdpSelectingStateController {
    void handleIdpSelected(final String idpEntityId, final String principalIpAddress, boolean registering, LevelOfAssurance requestedLoa);

    String getRequestIssuerId();

    AuthnRequestSignInProcess getSignInProcessDetails();
}
