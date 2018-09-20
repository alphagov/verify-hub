package uk.gov.ida.hub.policy.domain;

import java.util.Optional;

public final class CountryAuthenticationStatus {

    public enum Status {
        Success,
        Failure
    }

    private Status status;
    private String message = null;

    public static CountryAuthenticationStatus success() { return new CountryAuthenticationStatus(Status.Success);}

    @SuppressWarnings("unused") // needed for JAXB
    private CountryAuthenticationStatus() {
    }

    private CountryAuthenticationStatus(Status status) {
        this(status, null);
    }

    private CountryAuthenticationStatus(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatusCode() {
        return status;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CountryAuthenticationStatus countryAuthenticationStatus = (CountryAuthenticationStatus) o;

        return status == countryAuthenticationStatus.status;
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
