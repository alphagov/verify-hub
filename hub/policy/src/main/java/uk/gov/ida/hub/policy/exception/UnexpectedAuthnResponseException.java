package uk.gov.ida.hub.policy.exception;

import uk.gov.ida.hub.policy.domain.IdpIdaStatus;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import static java.text.MessageFormat.format;

public class UnexpectedAuthnResponseException extends RuntimeException {

    public UnexpectedAuthnResponseException(SessionId sessionId, String requestIssuerId, IdpIdaStatus.Status responseStatus, Class<? extends State> actualState) {
        super(format("SessionId: {0}, RequestIssuer: {1}, ResponseStatus: {2}, CurrentState: {3}, ExpectedState: IdpSelectedState",
                sessionId.getSessionId(), requestIssuerId, responseStatus.toString(), actualState.getSimpleName()));
    }
}
