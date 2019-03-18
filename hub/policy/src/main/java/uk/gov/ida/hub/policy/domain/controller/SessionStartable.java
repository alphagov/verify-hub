package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingState;

public interface SessionStartable extends State, IdpSelectingState {
}
