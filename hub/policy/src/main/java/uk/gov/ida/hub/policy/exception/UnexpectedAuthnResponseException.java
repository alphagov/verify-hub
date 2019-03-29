package uk.gov.ida.hub.policy.exception;

import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;

public class UnexpectedAuthnResponseException extends InvalidSessionStateException {

    public UnexpectedAuthnResponseException(SessionId sessionId, Class<? extends State> actualState) {
        super(sessionId, IdpSelectedState.class, actualState);
    }
}
