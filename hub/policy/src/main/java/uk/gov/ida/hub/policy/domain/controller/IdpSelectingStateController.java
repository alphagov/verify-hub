package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;

public interface IdpSelectingStateController {
    void handleIdpSelected(final String idpEntityId, final String principalIpAddress, boolean registering);

    String getRequestIssuerId();

    AuthnRequestSignInProcess getSignInProcessDetails();
}
