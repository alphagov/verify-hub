package uk.gov.ida.hub.policy.domain;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

@Immutable
public final class ResponseProcessingDetails {
    private SessionId sessionId;
    private ResponseProcessingStatus responseProcessingStatus;
    private String transactionEntityId;

    @SuppressWarnings("unused")//Needed by JAXB
    private ResponseProcessingDetails() {
    }

    public ResponseProcessingDetails(
            SessionId sessionId,
            ResponseProcessingStatus responseProcessingStatus,
            String transactionEntityId) {

        this.sessionId = sessionId;
        this.responseProcessingStatus = responseProcessingStatus;
        this.transactionEntityId = transactionEntityId;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public ResponseProcessingStatus getResponseProcessingStatus() {
        return responseProcessingStatus;
    }

    public String getTransactionEntityId() {
        return transactionEntityId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResponseProcessingDetails{");
        sb.append("sessionId=").append(sessionId);
        sb.append(", responseProcessingStatus=").append(responseProcessingStatus);
        sb.append(", transactionEntityId='").append(transactionEntityId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResponseProcessingDetails that = (ResponseProcessingDetails) o;

        return Objects.equals(sessionId, that.sessionId) &&
            responseProcessingStatus == that.responseProcessingStatus &&
            Objects.equals(transactionEntityId, that.transactionEntityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, responseProcessingStatus, transactionEntityId);
    }
}
