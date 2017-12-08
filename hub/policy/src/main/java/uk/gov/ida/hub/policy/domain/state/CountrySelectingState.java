package uk.gov.ida.hub.policy.domain.state;

import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.domain.State;

public interface CountrySelectingState extends State {
    Optional<String> getRelayState();
}
