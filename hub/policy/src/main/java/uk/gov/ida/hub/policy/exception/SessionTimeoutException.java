package uk.gov.ida.hub.policy.exception;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.domain.SessionId;

public class SessionTimeoutException extends RuntimeException {

    private final SessionId sessionId;
    private final String transactionEntityId;
    private final DateTime sessionExpiryTimestamp;
    private final String requestId;

    public SessionTimeoutException(String message, SessionId sessionId, String transactionEntityId, DateTime sessionExpiryTimestamp, String requestId) {
        super(message);
        this.sessionId = sessionId;
        this.transactionEntityId = transactionEntityId;
        this.sessionExpiryTimestamp = sessionExpiryTimestamp;
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public String getTransactionEntityId() {
        return transactionEntityId;
    }

    public DateTime getSessionExpiryTimestamp() {
        return sessionExpiryTimestamp;
    }
}
