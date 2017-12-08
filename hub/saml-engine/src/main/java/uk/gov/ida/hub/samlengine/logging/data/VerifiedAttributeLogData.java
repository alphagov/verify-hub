package uk.gov.ida.hub.samlengine.logging.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifiedAttributeLogData {

    private final boolean verified;
    private final String to;

    public VerifiedAttributeLogData(
        @JsonProperty("verified") boolean verified,
        @JsonProperty("to") String to
    ) {
        this.verified = verified;
        this.to = to;
    }

    public boolean isVerified() {
        return verified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VerifiedAttributeLogData that = (VerifiedAttributeLogData) o;

        if (verified != that.verified) return false;
        return to != null ? to.equals(that.to) : that.to == null;
    }

    @Override
    public int hashCode() {
        int result = (verified ? 1 : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "VerifiedAttributeLogData{" +
            "verified=" + verified +
            ", to='" + to + '\'' +
            '}';
    }

    public String getTo() {
        return to;
    }
}
