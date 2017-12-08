package uk.gov.ida.hub.policy.builder;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;

import java.net.URI;
import java.util.UUID;

public class AttributeQueryContainerDtoBuilder {

    private String id = "default-id";
    private String issuer = "default-issuer-id";
    private String samlRequest = UUID.randomUUID().toString();
    private URI matchingServiceUri = URI.create("/default-matching-service-uri");
    private DateTime attributeQueryClientTimeout = DateTime.now().plusSeconds(45);
    private boolean onboarding = false;

    public static AttributeQueryContainerDtoBuilder anAttributeQueryContainerDto() {
        return new AttributeQueryContainerDtoBuilder();
    }

    public AttributeQueryContainerDto build() {
        return new AttributeQueryContainerDto(samlRequest, matchingServiceUri, id, attributeQueryClientTimeout, issuer, onboarding);
    }

    public AttributeQueryContainerDtoBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }
}
