package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;

public class FraudDetectedDetailsBuilder {

    private String eventId = "default-event-id";
    private String fraudIndicator = "IT01";

    public static FraudDetectedDetailsBuilder aFraudDetectedDetails() {
        return new FraudDetectedDetailsBuilder();
    }

    public FraudDetectedDetails build() {
        return new FraudDetectedDetails(eventId, fraudIndicator);
    }

    public FraudDetectedDetailsBuilder withFraudIndicator(String fraudIndicator) {
        this.fraudIndicator = fraudIndicator;
        return this;
    }

    public FraudDetectedDetailsBuilder withFraudEventId(String eventId){
        this.eventId = eventId;
        return this;
    }
}
