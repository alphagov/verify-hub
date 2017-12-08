package uk.gov.ida.hub.policy.domain;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;

@Immutable
public final class Cycle3AttributeRequestData {
    private String attributeName;
    private String requestIssuerId;

    @SuppressWarnings("unused")//Needed by JAXB
    private Cycle3AttributeRequestData() {
    }

    public Cycle3AttributeRequestData(
        final String attributeName,
        final String requestIssuerId) {

        this.attributeName = attributeName;
        this.requestIssuerId = requestIssuerId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getRequestIssuerId() {
        return requestIssuerId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Cycle3AttributeRequestData{");
        sb.append("attributeName='").append(attributeName).append('\'');
        sb.append(", requestIssuerId='").append(requestIssuerId).append('\'');
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

        Cycle3AttributeRequestData that = (Cycle3AttributeRequestData) o;

        return Objects.equals(attributeName, that.attributeName) &&
            Objects.equals(requestIssuerId, that.requestIssuerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeName, requestIssuerId);
    }
}
