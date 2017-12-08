package uk.gov.ida.hub.policy.domain;

import java.util.Optional;

import static java.util.Optional.empty;

public final class IdpIdaStatus {
    public enum Status {
        Success,
        NoAuthenticationContext,
        RequesterError,
        AuthenticationFailed,
        AuthenticationCancelled,
        AuthenticationPending,
        UpliftFailed,
    }
    public static IdpIdaStatus success() { return new IdpIdaStatus(Status.Success);}

    private Status status;
    private Optional<String> message = empty();

    @SuppressWarnings("unused") // needed for JAXB
    private IdpIdaStatus() {
    }

    private IdpIdaStatus(Status status) {
        this(status, empty());
    }

    private IdpIdaStatus(Status status, Optional<String> message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatusCode() {
        return status;
    }

    public Optional<String> getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IdpIdaStatus idpIdaStatus = (IdpIdaStatus) o;

        if (status != idpIdaStatus.status) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result;
        return result;
    }

    @Override
    public String toString() {
        return "IdpIdaStatus{" +
                "status=" + status +
                ", message=" + message +
                '}';
    }
}
