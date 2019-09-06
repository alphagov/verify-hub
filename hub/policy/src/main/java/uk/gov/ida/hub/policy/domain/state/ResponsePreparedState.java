package uk.gov.ida.hub.policy.domain.state;

import uk.gov.ida.hub.policy.domain.State;

import java.util.Optional;

public interface ResponsePreparedState extends State {
    Optional<String> getRelayState();
}
