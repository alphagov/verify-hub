package uk.gov.ida.hub.policy.domain;

public interface StateTransitionAction {
    void transitionTo(State state);
}
