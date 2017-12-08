package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;

public class Cycle3AttributeRequestDataBuilder {
    private String attributeName = "attributeName";
    private String requestIssuerId = "default request issuer id";

    public static Cycle3AttributeRequestDataBuilder aCycle3AttributeRequestData() {
        return new Cycle3AttributeRequestDataBuilder();
    }

    public Cycle3AttributeRequestData build() {
        return new Cycle3AttributeRequestData(
                attributeName,
                requestIssuerId);
    }

    public Cycle3AttributeRequestDataBuilder withRequestIssuerId(String requestIssuerId) {
        this.requestIssuerId = requestIssuerId;
        return this;
    }

    public Cycle3AttributeRequestDataBuilder withAttributeName(String attributeName) {
        this.attributeName = attributeName;
        return this;
    }
}
