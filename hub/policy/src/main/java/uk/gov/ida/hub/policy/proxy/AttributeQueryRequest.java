package uk.gov.ida.hub.policy.proxy;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import java.net.URI;

public class AttributeQueryRequest {

    private String samlRequest;
    private URI matchingServiceUri;
    private DateTime attributeQueryClientTimeOut;
    private String id;
    private String issuer;
    private boolean onboarding;

    @SuppressWarnings("unused") //Required by JAXB
    private AttributeQueryRequest() {}

    public AttributeQueryRequest(
            String id,
            String issuer,
            String samlRequest,
            URI matchingServiceUri,
            DateTime attributeQueryClientTimeOut,
            boolean onboarding) {

        this.id = id;
        this.issuer = issuer;
        this.samlRequest = samlRequest;
        this.matchingServiceUri = matchingServiceUri;
        this.onboarding = onboarding;
        this.attributeQueryClientTimeOut = attributeQueryClientTimeOut;
    }

    public URI getMatchingServiceUri() {
        return matchingServiceUri;
    }

    public String getId() {
        return id;
    }

    public String getIssuer() {
        return issuer;
    }

    public DateTime getAttributeQueryClientTimeOut() { return attributeQueryClientTimeOut; }

    public boolean isOnboarding() {
        return onboarding;
    }

    public String getSamlRequest() {
        return samlRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AttributeQueryRequest that = (AttributeQueryRequest) o;

        return new EqualsBuilder()
            .append(onboarding, that.onboarding)
            .append(samlRequest, that.samlRequest)
            .append(matchingServiceUri, that.matchingServiceUri)
            .append(attributeQueryClientTimeOut, that.attributeQueryClientTimeOut)
            .append(id, that.id)
            .append(issuer, that.issuer)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(samlRequest)
            .append(matchingServiceUri)
            .append(attributeQueryClientTimeOut)
            .append(id)
            .append(issuer)
            .append(onboarding)
            .toHashCode();
    }
}
