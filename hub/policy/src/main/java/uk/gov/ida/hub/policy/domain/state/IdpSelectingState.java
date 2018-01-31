package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.State;

public interface IdpSelectingState extends State {
    Optional<Boolean> getForceAuthentication();
    Optional<String> getRelayState();
}
