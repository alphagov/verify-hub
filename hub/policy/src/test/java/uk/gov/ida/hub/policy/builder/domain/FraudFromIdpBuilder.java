package uk.gov.ida.hub.policy.builder.domain;

import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.FraudFromIdp;
import uk.gov.ida.hub.policy.domain.PersistentId;

import java.util.Optional;
import java.util.UUID;

import static uk.gov.ida.hub.policy.builder.domain.FraudDetectedDetailsBuilder.aFraudDetectedDetails;
import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;

public class FraudFromIdpBuilder {

    private String issuer = "issuer";
    private String principalIpAddressAsSeenByHub = "principal ip address as seen by hub";
    private PersistentId persistentId = aPersistentId().build();
    private FraudDetectedDetails fraudDetectedDetails = aFraudDetectedDetails().build();
    private Optional<String> principalIpAddressAsSeenByIdp = Optional.empty();
    private String analyticsSessionId = UUID.randomUUID().toString();
    private String journeyType = "some-journey-type";

    public static FraudFromIdpBuilder aFraudFromIdp() {
        return new FraudFromIdpBuilder();
    }

    public FraudFromIdp build() {
        return new FraudFromIdp(issuer, principalIpAddressAsSeenByHub, persistentId, fraudDetectedDetails, principalIpAddressAsSeenByIdp, analyticsSessionId, journeyType);
    }


    public FraudFromIdpBuilder withIssuerId(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public FraudFromIdpBuilder withPrincipalIpAddressAsSeenByHub(String principalIpAddressAsSeenByHub) {
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
        return this;
    }

    public FraudFromIdpBuilder withFraudDetectedDetails(FraudDetectedDetails fraudDetectedDetails) {
        this.fraudDetectedDetails = fraudDetectedDetails;
        return this;
    }

    public FraudFromIdpBuilder withPrincipalIpAddressSeenByIdp(String principalIpAddressAsSeenByIdp) {
        this.principalIpAddressAsSeenByIdp = Optional.ofNullable(principalIpAddressAsSeenByIdp);
        return this;
    }

    public FraudFromIdpBuilder withAnalyticsSessionId(String analyticsSessionId) {
        this.analyticsSessionId = analyticsSessionId;
        return this;
    }

    public FraudFromIdpBuilder withJourneyType(String journeyType) {
        this.journeyType = journeyType;
        return this;
    }
}
