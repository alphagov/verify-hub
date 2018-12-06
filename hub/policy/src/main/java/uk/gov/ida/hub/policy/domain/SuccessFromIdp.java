package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;

public class SuccessFromIdp {

    private String issuer;
    private String encryptedMatchingDatasetAssertion;
    private String encryptedAuthnAssertion;
    private PersistentId persistentId;
    private LevelOfAssurance levelOfAssurance;
    private String principalIpAddressAsSeenByHub;
    private Optional<String> principalIpAddressAsSeenByIdp;
    private String analyticSessionId;
    private String journeyType;

    @SuppressWarnings("unused")//Needed by JAXB
    private SuccessFromIdp() {
    }

    public SuccessFromIdp(
            String issuer,
            String encryptedMatchingDatasetAssertion,
            String encryptedAuthnAssertion,
            PersistentId persistentId,
            LevelOfAssurance levelOfAssurance,
            String principalIpAddressAsSeenByHub,
            Optional<String> principalIpAddressAsSeenByIdp,
            String analyticsSessionId,
            String journeyType) {

        this.issuer = issuer;
        this.encryptedMatchingDatasetAssertion = encryptedMatchingDatasetAssertion;
        this.encryptedAuthnAssertion = encryptedAuthnAssertion;
        this.persistentId = persistentId;
        this.levelOfAssurance = levelOfAssurance;
        this.principalIpAddressAsSeenByHub = principalIpAddressAsSeenByHub;
        this.principalIpAddressAsSeenByIdp = principalIpAddressAsSeenByIdp;
        this.analyticSessionId = analyticsSessionId;
        this.journeyType = journeyType;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getEncryptedAuthnAssertion() {
        return encryptedAuthnAssertion;
    }

    public PersistentId getPersistentId() {
        return persistentId;
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public String getPrincipalIpAddressAsSeenByHub() {
        return principalIpAddressAsSeenByHub;
    }

    public Optional<String> getPrincipalIpAddressAsSeenByIdp() {
        return principalIpAddressAsSeenByIdp;
    }

    public String getEncryptedMatchingDatasetAssertion() {
        return encryptedMatchingDatasetAssertion;
    }

    public String getAnalyticSessionId() {
        return analyticSessionId;
    }

    public String getJourneyType() {
        return journeyType;
    }
}
