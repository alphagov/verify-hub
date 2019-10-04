package uk.gov.ida.hub.samlsoapproxy.builders;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.AttributeQuery;
import uk.gov.ida.hub.samlsoapproxy.domain.AttributeQueryContainerDto;
import uk.gov.ida.shared.utils.xml.XmlUtils;

import java.net.URI;

public class AttributeQueryContainerDtoBuilder {

    private String id = "default-id";
    private String issuer = "default-issuer-id";
    private String samlRequest;
    private URI matchingServiceUri = URI.create("/default-matching-service-uri");
    private DateTime attributeQueryClientTimeout = DateTime.now().plusSeconds(45);
    private boolean onboarding = false;

    private AttributeQueryContainerDtoBuilder(AttributeQuery attributeQuery) {
        this.samlRequest = XmlUtils.writeToString(attributeQuery.getDOM());
    }

    public static AttributeQueryContainerDtoBuilder anAttributeQueryContainerDto(AttributeQuery attributeQuery) {
        return new AttributeQueryContainerDtoBuilder(attributeQuery);
    }

    public AttributeQueryContainerDto build() {
        return new AttributeQueryContainerDto(id, issuer, samlRequest, matchingServiceUri, attributeQueryClientTimeout, onboarding);
    }

    public AttributeQueryContainerDtoBuilder withMatchingServiceUri(URI matchingServiceUri) {
        this.matchingServiceUri = matchingServiceUri;
        return this;
    }

    public AttributeQueryContainerDtoBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public AttributeQueryContainerDtoBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public AttributeQueryContainerDtoBuilder withAttributeQueryClientTimeout(DateTime timeout){
        this.attributeQueryClientTimeout = timeout;
        return this;
    }
}
