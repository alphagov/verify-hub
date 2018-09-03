package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;

public class UserAccountCreationFailedStateController extends AbstractUserAccountCreationFailedStateController<UserAccountCreationFailedState> {

    public UserAccountCreationFailedStateController(
            final UserAccountCreationFailedState state,
            final ResponseFromHubFactory responseFromHubFactory) {

        super(state, responseFromHubFactory);
    }
}
