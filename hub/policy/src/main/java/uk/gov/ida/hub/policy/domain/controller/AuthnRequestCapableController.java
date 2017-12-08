package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.AuthnRequestFromHub;

public interface AuthnRequestCapableController {
    AuthnRequestFromHub getRequestFromHub();
}
