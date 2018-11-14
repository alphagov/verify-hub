package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;

import java.util.Collection;

public class SuccessfulMatchStateController extends AbstractSuccessfulMatchStateController<SuccessfulMatchState> {

    public SuccessfulMatchStateController(
            final SuccessfulMatchState state,
            final ResponseFromHubFactory responseFromHubFactory) {
        super(state, responseFromHubFactory);
    }

    @Override
    public ResponseFromHub getPreparedResponse() {
        return getResponse();
    }
}
