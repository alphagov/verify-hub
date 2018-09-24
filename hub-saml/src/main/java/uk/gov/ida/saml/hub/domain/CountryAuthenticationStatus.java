package uk.gov.ida.saml.hub.domain;

import uk.gov.ida.saml.core.domain.IdaStatus;

import java.util.Optional;

public final class CountryAuthenticationStatus implements IdaStatus {

    public enum Status {
        Success,
        Failure
    }

    private Status status;
    private String message;

    public static CountryAuthenticationStatus success() { return new CountryAuthenticationStatus(Status.Success);}
    public static CountryAuthenticationStatus failure() { return new CountryAuthenticationStatus(Status.Failure);}

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

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
        return "CountryAuthenticationStatus{" +
            "status=" + getStatusCode() +
            ", message=" + getMessage() +
            '}';
    }

    public static class CountryAuthenticationStatusFactory implements AuthenticationStatusFactory<Status, CountryAuthenticationStatus> {
        public CountryAuthenticationStatus create(final CountryAuthenticationStatus.Status statusCode, final String message) {

            if (!statusCode.equals(Status.Failure)) {
                return new CountryAuthenticationStatus(statusCode);
            }

            return new CountryAuthenticationStatus(statusCode, message);
        }
    }
}
