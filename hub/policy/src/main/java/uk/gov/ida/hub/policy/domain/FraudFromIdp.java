package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;

public class FraudFromIdp {

    private String issuer;
    private String principalIpAddressSeenByHub;
    private PersistentId persistentId;
    private FraudDetectedDetails fraudDetectedDetails;
    private Optional<String> principalIpAddressAsSeenByIdp;

    @SuppressWarnings("unused")//Needed by JAXB
    private FraudFromIdp() {
    }

    public FraudFromIdp(String issuer, String principalIpAddressSeenByHub, PersistentId persistentId, FraudDetectedDetails fraudDetectedDetails, Optional<String> principalIpAddressAsSeenByIdp) {
        this.issuer = issuer;
        this.principalIpAddressSeenByHub = principalIpAddressSeenByHub;
        this.persistentId = persistentId;
        this.fraudDetectedDetails = fraudDetectedDetails;
        this.principalIpAddressAsSeenByIdp = principalIpAddressAsSeenByIdp;
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public FraudDetectedDetails getFraudDetectedDetails() {
        return fraudDetectedDetails;
    }

    public Optional<String> getPrincipalIpAddressAsSeenByIdp() {
        return principalIpAddressAsSeenByIdp;
    }

    public String getIssuer() {
        return issuer;
    }


    public String getPrincipalIpAddressSeenByHub() {
        return principalIpAddressSeenByHub;
    }
}
