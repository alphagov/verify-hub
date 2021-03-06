package uk.gov.ida.hub.samlsoapproxy.domain;

import org.joda.time.DateTime;

import java.net.URI;

public class AttributeQueryContainerDto {
    private String samlRequest;
    private URI matchingServiceUri;
    private DateTime attributeQueryClientTimeOut;
    private String id;
    private String issuer;
    private boolean onboarding;

    @SuppressWarnings("unused") //Required by JAXB
    private AttributeQueryContainerDto() {}

    public AttributeQueryContainerDto(
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

    public String getSamlRequest() {
        return samlRequest;
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

}
